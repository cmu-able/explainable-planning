package solver.prismconnector;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarBoolean;
import language.domain.models.IStateVarInt;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.IncompatibleActionException;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.ActionSpace;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.FactoredPSO;
import language.mdp.IActionDescription;
import language.mdp.IStateVarTuple;
import language.mdp.ProbabilisticEffect;
import language.mdp.ProbabilisticTransition;
import language.mdp.StateSpace;
import language.mdp.StateVarTuple;
import language.mdp.TabularActionDescription;
import language.mdp.TransitionFunction;

public class PrismTranslatorHelper {
	private ValueEncodingScheme mEncodings;

	public PrismTranslatorHelper(ValueEncodingScheme encodings) {
		mEncodings = encodings;
	}

	/**
	 * Build constants' declarations for values of variables of types unsupported by PRISM language.
	 * 
	 * @param stateSpace
	 * @return const int {varName}_{value} = {encoded int value}; ...
	 * @throws VarNotFoundException
	 */
	String buildConstsDecl(StateSpace stateSpace) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateSpace) {
			String varName = stateVarDef.getName();
			builder.append("// Possible values of ");
			builder.append(varName);
			builder.append("\n");
			for (IStateVarValue value : stateVarDef.getPossibleValues()) {
				if (value instanceof IStateVarBoolean || value instanceof IStateVarInt) {
					break;
				}
				Integer encodedValue = mEncodings.getEncodedIntValue(stateVarDef, value);
				builder.append("const int ");
				builder.append(varName);
				builder.append("_");
				String valueString = String.valueOf(value);
				builder.append(PrismTranslatorUtils.sanitizeNameString(valueString));
				builder.append(" = ");
				builder.append(encodedValue);
				builder.append(";");
				builder.append("\n");
			}
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * Build a goal formula.
	 * 
	 * @param goal
	 * @return formula goal = {goal expression};
	 * @throws VarNotFoundException
	 */
	String buildGoalDecl(StateVarTuple goal) throws VarNotFoundException {
		String goalExpr = PrismTranslatorUtils.buildExpression(goal, mEncodings);
		StringBuilder builder = new StringBuilder();
		builder.append("formula goal = ");
		builder.append(goalExpr);
		builder.append(";");
		return builder.toString();
	}

	/**
	 * Build an "end" label.
	 * 
	 * @param goal
	 * @return label "end" = {goal expression} & !computeGo & barrier;
	 * @throws VarNotFoundException
	 */
	String buildEndDecl(StateVarTuple goal) throws VarNotFoundException {
		String goalExpr = PrismTranslatorUtils.buildExpression(goal, mEncodings);
		StringBuilder builder = new StringBuilder();
		builder.append("label \"end\" = ");
		builder.append(goalExpr);
		builder.append(" & !computeGo & barrier;");
		return builder.toString();
	}

	/**
	 * Build a helper module that handles cycles of choosing action, reward computation, checking if the goal (if any)
	 * is reached for termination.
	 * 
	 * @param actionDefs
	 * @param helperActionFilter
	 *            : A function that filters actions of the helper module
	 * @param hasGoal
	 * @return A helper module that handles cycles of choosing action, reward computation, checking if the goal is
	 *         reached for termination
	 * @throws VarNotFoundException
	 */
	String buildHelperModule(ActionSpace actionDefs, ActionFilter helperActionFilter, boolean hasGoal)
			throws VarNotFoundException {
		return buildHelperModule(actionDefs, helperActionFilter, hasGoal, null);
	}

	/**
	 * Build a helper module that handles cycles of choosing action, reward computation, checking if the goal (if any)
	 * is reached for termination.
	 * 
	 * Additionally, the helper module handles a "query state", which is a source state of a user's why-not question.
	 * This query state is to be made absorbing, so that the effect of the why-not action is not reverted by the
	 * planner.
	 * 
	 * There may be multiple query states, all of which are to be made absorbing.
	 * 
	 * @param actionDefs
	 * @param helperActionFilter
	 *            : A function that filters actions of the helper module
	 * @param hasGoal
	 * @param queryStates
	 *            : null if there is no why-not query
	 * @return A helper module that handles cycles of choosing action, reward computation, checking if the goal is
	 *         reached for termination
	 * @throws VarNotFoundException
	 */
	String buildHelperModule(ActionSpace actionDefs, ActionFilter helperActionFilter, boolean hasGoal,
			Set<StateVarTuple> queryStates) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();

		// Check if there is a why-not query state to handle
		String queryStateFormula;
		if (queryStates != null) {
			String queryStateExpr = PrismTranslatorUtils.buildExpression(queryStates, mEncodings);
			queryStateFormula = "formula query_state = " + queryStateExpr + ";";
		} else {
			queryStateFormula = "formula query_state = false;";
		}

		// Add query state formula
		builder.append(queryStateFormula);
		builder.append("\n\n");

		// module helper [...] endmodule
		builder.append("module helper");
		builder.append("\n");

		if (hasGoal) {
			builder.append(PrismTranslatorUtils.INDENT);
			builder.append("barrier : bool init false;");
			builder.append("\n");
		}

		builder.append(PrismTranslatorUtils.INDENT);
		builder.append("computeGo : bool init false;");
		builder.append("\n\n");

		for (ActionDefinition<IAction> actionDef : actionDefs) {

			for (IAction action : actionDef.getActions()) {
				if (!helperActionFilter.filterAction(action)) {
					// Skip actions that are not present in the model (in the case of DTMC)
					continue;
				}

				String sanitizedActionName = PrismTranslatorUtils.sanitizeNameString(action.getName());
				builder.append(PrismTranslatorUtils.INDENT);
				builder.append("[");
				builder.append(sanitizedActionName);
				builder.append("]");

				if (hasGoal) {
					builder.append(" !computeGo & !barrier -> (computeGo'=true) & (barrier'=true);");
				} else {
					builder.append(" !computeGo -> (computeGo'=true);");
				}
				builder.append("\n");
			}
		}

		builder.append("\n");
		if (hasGoal) {
			// Compute cost value and continue if not in query state
			builder.append(PrismTranslatorUtils.INDENT);
			builder.append("[compute] computeGo & barrier & !query_state -> (computeGo'=false);");
			builder.append("\n");

			// Make query state absorbing state
			builder.append(PrismTranslatorUtils.INDENT);
			builder.append("[absorb] computeGo & barrier & query_state -> true;");
			builder.append("\n");

			builder.append(PrismTranslatorUtils.INDENT);
			builder.append("[next] !computeGo & barrier & !goal -> (barrier'=false);");
			builder.append("\n");
			builder.append(PrismTranslatorUtils.INDENT);
			builder.append("[end] !computeGo & barrier & goal -> true;");
			builder.append("\n");
		} else {
			builder.append(PrismTranslatorUtils.INDENT);
			builder.append("[compute] computeGo -> (computeGo'=false);");
			builder.append("\n");
		}
		builder.append("endmodule");
		return builder.toString();
	}

	/**
	 * 
	 * @param moduleVarSpace
	 *            : Variables of the module
	 * @param iniState
	 *            : Initial state
	 * @return {varName} : [0..{maximum encoded int}] init {encoded int initial value}; ...
	 * @throws VarNotFoundException
	 */
	String buildModuleVarsDecl(StateSpace moduleVarSpace, StateVarTuple iniState) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (StateVarDefinition<IStateVarValue> stateVarDef : moduleVarSpace) {
			IStateVarValue iniValue = iniState.getStateVarValue(IStateVarValue.class, stateVarDef);
			String varDecl;
			if (iniValue instanceof IStateVarBoolean) {
				StateVarDefinition<IStateVarBoolean> boolVarDef = castTypeStateVarDef(stateVarDef,
						IStateVarBoolean.class);
				varDecl = buildBooleanModuleVarDecl(boolVarDef, (IStateVarBoolean) iniValue);
			} else if (iniValue instanceof IStateVarInt) {
				StateVarDefinition<IStateVarInt> intVarDef = castTypeStateVarDef(stateVarDef, IStateVarInt.class);
				varDecl = buildIntModuleVarDecl(intVarDef, (IStateVarInt) iniValue);
			} else {
				varDecl = buildModuleVarDecl(stateVarDef, iniValue);
			}

			if (!first) {
				builder.append("\n");
			} else {
				first = false;
			}
			builder.append(PrismTranslatorUtils.INDENT);
			builder.append(varDecl);
		}
		return builder.toString();
	}

	/**
	 * Build a variable declaration of a module, where the variable type is unsupported by PRISM language.
	 * 
	 * @param varDef
	 * @param iniValue
	 * @return {varName} : [0..{maximum encoded int}] init {encoded int initial value};
	 * @throws VarNotFoundException
	 */
	String buildModuleVarDecl(StateVarDefinition<IStateVarValue> varDef, IStateVarValue iniValue)
			throws VarNotFoundException {
		Integer maxEncodedValue = mEncodings.getMaximumEncodedIntValue(varDef);
		Integer encodedIniValue = mEncodings.getEncodedIntValue(varDef, iniValue);
		StringBuilder builder = new StringBuilder();
		builder.append(varDef.getName());
		builder.append(" : [0..");
		builder.append(maxEncodedValue);
		builder.append("] init ");
		builder.append(encodedIniValue);
		builder.append(";");
		return builder.toString();
	}

	/**
	 * 
	 * @param boolVarDef
	 * @param iniValBoolean
	 * @return {varName} : bool init {initial value};
	 */
	String buildBooleanModuleVarDecl(StateVarDefinition<IStateVarBoolean> boolVarDef, IStateVarBoolean iniValBoolean) {
		StringBuilder builder = new StringBuilder();
		builder.append(boolVarDef.getName());
		builder.append(" : bool init ");
		builder.append(iniValBoolean.getValue() ? "true" : "false");
		builder.append(";");
		return builder.toString();
	}

	/**
	 * 
	 * @param intVarDef
	 * @param iniValInt
	 * @return {varName} : [{min}..{max}] init {initial value};
	 */
	String buildIntModuleVarDecl(StateVarDefinition<IStateVarInt> intVarDef, IStateVarInt iniValInt) {
		StringBuilder builder = new StringBuilder();
		builder.append(intVarDef.getName());
		Comparator<IStateVarInt> comparator = (var1, var2) -> var1.getValue() - var2.getValue();
		IStateVarInt lowerBound = Collections.min(intVarDef.getPossibleValues(), comparator);
		IStateVarInt uppberBound = Collections.max(intVarDef.getPossibleValues(), comparator);
		builder.append(" : [");
		builder.append(lowerBound.getValue());
		builder.append("..");
		builder.append(uppberBound.getValue());
		builder.append("] init ");
		builder.append(iniValInt.getValue());
		builder.append(";");
		return builder.toString();
	}

	/**
	 * 
	 * @param stateSpace
	 *            : State space of the (corresponding) MDP
	 * @param iniState
	 *            : Initial state
	 * @param actionDefs
	 *            : Definitions of actions that are present in this model (either MDP or DTMC)
	 * @param actionPSOs
	 *            : PSOs of actions that are present in this model (either MDP or DTMC)
	 * @param partialCommandsBuilder
	 *            : A function that builds partial commands of a module, given an action description
	 * @param hasGoal
	 *            : Whether the MDP has a goal
	 * @return module {name} {vars decl} {commands} endmodule ...
	 * @throws XMDPException
	 */
	String buildModules(StateSpace stateSpace, StateVarTuple iniState, ActionSpace actionDefs,
			TransitionFunction actionPSOs, PartialModuleCommandsBuilder partialCommandsBuilder, boolean hasGoal)
			throws XMDPException {
		// This determines a set of module variables. Each set of variables are updated independently.
		// These variables are updated by some actions in the model.
		Set<ChainOfEffectClasses> chainsOfEffectClasses = getChainsOfEffectClasses(actionPSOs);

		// These variables are unmodified by the actions in the model.
		// This is mostly for handling DTMC.
		StateSpace unmodifiedVarSpace = stateSpace;

		StringBuilder builder = new StringBuilder();
		int moduleCount = 0;
		boolean first = true;

		for (ChainOfEffectClasses chain : chainsOfEffectClasses) {
			moduleCount++;
			StateSpace moduleVarSpace = new StateSpace();
			Map<FactoredPSO<IAction>, Set<EffectClass>> moduleActionPSOs = new HashMap<>();

			int chainLength = chain.getChainLength();
			for (int i = 0; i < chainLength; i++) {
				EffectClass effectClass = chain.getEffectClass(i);
				FactoredPSO<IAction> actionPSO = chain.getFactoredPSO(i);

				moduleVarSpace.addStateVarDefinitions(effectClass);

				if (!moduleActionPSOs.containsKey(actionPSO)) {
					moduleActionPSOs.put(actionPSO, new HashSet<>());
				}
				moduleActionPSOs.get(actionPSO).add(effectClass);
			}

			unmodifiedVarSpace = unmodifiedVarSpace.getDifference(moduleVarSpace);

			String module = buildModule("module_" + moduleCount, moduleVarSpace, iniState, moduleActionPSOs,
					partialCommandsBuilder);

			if (!first) {
				builder.append("\n\n");
			} else {
				first = false;
			}
			builder.append(module);
		}

		if (!unmodifiedVarSpace.isEmpty()) {
			moduleCount++;
			Map<FactoredPSO<IAction>, Set<EffectClass>> emptyActionPSOs = new HashMap<>();
			String noCommandModule = buildModule("module_" + moduleCount, unmodifiedVarSpace, iniState, emptyActionPSOs,
					partialCommandsBuilder);
			builder.append("\n\n");
			builder.append(noCommandModule);
		}

		return builder.toString();
	}

	/**
	 * 
	 * @param moduleName
	 *            : A unique name of the module
	 * @param moduleVarSpace
	 *            : Variables of the module
	 * @param iniState
	 *            : Initial state
	 * @param actionPSOs
	 *            : A mapping from each action PSO to (a subset of) its effect classes that are "chained" by other
	 *            effect classes of other action types
	 * @param partialCommandsBuilder
	 *            : A function that builds partial commands of a module, given an action description
	 * @return module {name} {vars decl} {commands} endmodule
	 * @throws XMDPException
	 */
	String buildModule(String moduleName, StateSpace moduleVarSpace, StateVarTuple iniState,
			Map<FactoredPSO<IAction>, Set<EffectClass>> actionPSOs, PartialModuleCommandsBuilder partialCommandsBuilder)
			throws XMDPException {
		String varsDecl = buildModuleVarsDecl(moduleVarSpace, iniState);
		String commands = buildModuleCommands(actionPSOs, partialCommandsBuilder);

		StringBuilder builder = new StringBuilder();
		builder.append("module ");
		builder.append(moduleName);
		builder.append("\n");
		builder.append(varsDecl);
		builder.append("\n\n");
		builder.append(commands);
		builder.append("\n");
		builder.append("endmodule");
		return builder.toString();
	}

	/**
	 * Build all commands of a module -- for MDP or DTMC.
	 * 
	 * @param actionPSOs
	 *            : A mapping from each action PSO to (a subset of) its effect classes that are "chained" by other
	 *            effect classes of other action types
	 * @param partialCommandsBuilder
	 *            : A function that builds partial commands of a module, given an action description
	 * @return all commands of the module in the form [actionX] {guard_1} -> {updates_1}; ... [actionZ] {guard_p} ->
	 *         {updates_p};
	 * @throws XMDPException
	 */
	String buildModuleCommands(Map<FactoredPSO<IAction>, Set<EffectClass>> actionPSOs,
			PartialModuleCommandsBuilder partialCommandsBuilder) throws XMDPException {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Entry<FactoredPSO<IAction>, Set<EffectClass>> entry : actionPSOs.entrySet()) {
			FactoredPSO<IAction> actionPSO = entry.getKey();
			Set<EffectClass> chainedEffectClasses = entry.getValue();
			IActionDescription<IAction> actionDesc;
			if (chainedEffectClasses.size() > 1) {
				actionDesc = mergeActionDescriptions(actionPSO, chainedEffectClasses);
			} else {
				EffectClass effectClass = chainedEffectClasses.iterator().next();
				actionDesc = actionPSO.getActionDescription(effectClass);
			}
			String actionDefName = actionPSO.getActionDefinition().getName();
			String commands = partialCommandsBuilder.buildPartialModuleCommands(actionDesc);
			if (!first) {
				builder.append("\n\n");
			} else {
				first = false;
			}
			builder.append(PrismTranslatorUtils.INDENT);
			builder.append("// ");
			builder.append(actionDefName);
			builder.append("\n");
			builder.append(commands);
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param action
	 * @param predicate
	 * @param probEffect
	 * @return [actionName] {guard} -> {updates};
	 * @throws VarNotFoundException
	 */
	String buildModuleCommand(IAction action, IStateVarTuple predicate, ProbabilisticEffect probEffect)
			throws VarNotFoundException {
		String guard = PrismTranslatorUtils.buildExpression(predicate, mEncodings);
		String updates = buildUpdates(probEffect);

		StringBuilder builder = new StringBuilder();
		builder.append("[");
		String sanitizedActionName = PrismTranslatorUtils.sanitizeNameString(action.getName());
		builder.append(sanitizedActionName);
		builder.append("] ");
		builder.append(guard);
		builder.append(" -> ");
		builder.append(updates);
		builder.append(";");
		return builder.toString();
	}

	/**
	 * 
	 * @param probEffects
	 * @return {prob_1}:{update_1} + ... + {prob_k}:{update_k}
	 * @throws VarNotFoundException
	 */
	String buildUpdates(ProbabilisticEffect probEffects) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		boolean firstBranch = true;
		for (Entry<Effect, Double> entry : probEffects) {
			Effect effect = entry.getKey();
			Double prob = entry.getValue();
			if (!firstBranch) {
				builder.append(" + ");
			} else {
				firstBranch = false;
			}
			builder.append(prob);
			builder.append(":");
			builder.append(buildUpdate(effect));
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param update
	 * @return {var_1 update}&...&{var_n update}
	 * @throws VarNotFoundException
	 */
	String buildUpdate(Effect update) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		boolean firstVar = true;
		for (StateVar<IStateVarValue> stateVar : update) {
			String varUpdate = buildVariableUpdate(stateVar);
			if (!firstVar) {
				builder.append("&");
			} else {
				firstVar = false;
			}
			builder.append(varUpdate);
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param updatedStateVar
	 * @return ({varName}'={value OR encoded int value})
	 * @throws VarNotFoundException
	 */
	String buildVariableUpdate(StateVar<IStateVarValue> updatedStateVar) throws VarNotFoundException {
		String varName = updatedStateVar.getName();
		IStateVarValue value = updatedStateVar.getValue();
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(varName);
		builder.append("'");
		builder.append("=");

		if (value instanceof IStateVarInt || value instanceof IStateVarBoolean) {
			builder.append(value);
		} else {
			Integer encodedValue = mEncodings.getEncodedIntValue(updatedStateVar.getDefinition(), value);
			builder.append(encodedValue);
		}
		builder.append(")");
		return builder.toString();
	}

	private <E extends IStateVarValue> StateVarDefinition<E> castTypeStateVarDef(
			StateVarDefinition<IStateVarValue> genericVarDef, Class<E> valueType) {
		Set<E> possibleValues = new HashSet<>();
		for (IStateVarValue value : genericVarDef.getPossibleValues()) {
			possibleValues.add(valueType.cast(value));
		}
		return new StateVarDefinition<>(genericVarDef.getName(), possibleValues);
	}

	/**
	 * 
	 * @param actionPSOs
	 *            : PSOs of actions that are present in this model (either MDP or DTMC)
	 * @return A set of "chains" of effect classes. Each effect class is mapped to its corresponding action PSO. Two
	 *         (NOT necessarily unique) effect classes are "chained" iff: (1)~they are associated with different action
	 *         types, but they have overlapping state variables, or (2)~they are associated with the same action type
	 *         (by definition, they do not overlap), but they overlap with other effect classes of other action types
	 *         that are "chained".
	 */
	Set<ChainOfEffectClasses> getChainsOfEffectClasses(TransitionFunction actionPSOs) {
		// This set of chains will be built iteratively
		Set<ChainOfEffectClasses> currChains = new HashSet<>();

		boolean firstPSO = true;
		for (FactoredPSO<IAction> actionPSO : actionPSOs) {
			Set<EffectClass> actionEffectClasses = actionPSO.getIndependentEffectClasses();
			if (firstPSO) {
				// For the 1st actionPSO, establish an initial set of chains, with 1 element in each chain
				// (all effect classes of a single actionPSO are non-overlapping by definition)

				for (EffectClass effectClass : actionEffectClasses) {
					ChainOfEffectClasses iniChain = new ChainOfEffectClasses();

					// Put 1st element into the chain
					iniChain.addEffectClass(effectClass, actionPSO);

					// Add this initial chain to the current set of chains
					currChains.add(iniChain);
				}
				firstPSO = false;
				continue;
			}

			// Once an initial set of length-1 chains is established, for the remaining actionPSOs, add their effect
			// classes to the existing chains
			for (EffectClass effectClass : actionEffectClasses) {
				currChains = getChainsOfEffectClassesHelper(currChains, effectClass, actionPSO);
			}
		}
		return currChains;
	}

	/**
	 * Add the given effectClass-actionPSO pair to the current set of "chains".
	 * 
	 * This method does not modify the current set of existing chains.
	 * 
	 * @param currChains
	 * @param effectClass
	 * @param actionPSO
	 * @return
	 */
	private Set<ChainOfEffectClasses> getChainsOfEffectClassesHelper(Set<ChainOfEffectClasses> currChains,
			EffectClass effectClass, FactoredPSO<IAction> actionPSO) {
		// This is the resulting set of chains
		Set<ChainOfEffectClasses> res = new HashSet<>();

		// This new chain will contain the given effectClass -- either by itself or with other (existing) chained effect
		// classes
		// This new chain will be built iteratively
		ChainOfEffectClasses newChain = new ChainOfEffectClasses();

		// Go over each existing chain to find if the given effectClass overlaps with it
		for (ChainOfEffectClasses currChain : currChains) {
			boolean overlapped = false;
			int chainLength = currChain.getChainLength();

			for (int i = 0; i < chainLength; i++) {
				// Check each effectClass in the existing chain
				EffectClass currEffectClass = currChain.getEffectClass(i);

				if (effectClass.overlaps(currEffectClass)) {
					// The given effectClass overlaps with the existing chained effectClass
					// Copy the current chain into the new chain -- the given effectClass will be added to it
					// later
					newChain.addChain(currChain);

					// Go to the next existing chain
					overlapped = true;
					break;
				}
			}

			// If the given effectClass doesn't overlap with this existing chain, this chain is left unmodified
			if (!overlapped) {
				// Copy this chain -- as is -- to the resulting set
				res.add(currChain);
			}

			// Check the next existing chain, because it is possible that the given effectClass overlaps with 2+
			// separate chains
			// In that case, those separate chains need to be chained together
		}

		// At this point, the new chain may be:
		// (a)~empty if the given effectClass doesn't overlap with any existing chain, or
		// (b)~comprises of 1 or more existing chains that the given effectClass overlaps

		// Add the given effectClass to the new chain
		newChain.addEffectClass(effectClass, actionPSO);

		// Add the new chain to the resulting set
		res.add(newChain);

		// At this point, the resulting set contains all the unmodified chains from the original set, plus some a new
		// chain containing the given effectClass
		return res;
	}

	/**
	 * 
	 * @param actionPSO
	 *            : An action PSO whose (some of) effect classes are "chained"
	 * @param chainedEffectClasses
	 *            : A subset of effect classes of actionPSO that are "chained"
	 * @return An action description of a merged effect class of chainedEffectClasses
	 * @throws XMDPException
	 */
	private IActionDescription<IAction> mergeActionDescriptions(FactoredPSO<IAction> actionPSO,
			Set<EffectClass> chainedEffectClasses) throws XMDPException {
		ActionDefinition<IAction> actionDef = actionPSO.getActionDefinition();

		TabularActionDescription<IAction> mergedActionDesc = new TabularActionDescription<>(actionDef);
		Set<ProbabilisticTransition<IAction>> mergedProbTransitions = new HashSet<>();

		for (IAction action : actionDef.getActions()) {
			for (EffectClass effectClass : chainedEffectClasses) {
				IActionDescription<IAction> actionDesc = actionPSO.getActionDescription(effectClass);
				Set<ProbabilisticTransition<IAction>> probTransitions = actionDesc.getProbabilisticTransitions(action);
				mergedProbTransitions = merge(action, mergedProbTransitions, probTransitions);
			}
			mergedActionDesc.putAll(mergedProbTransitions);
		}
		return mergedActionDesc;
	}

	/**
	 * 
	 * @param action
	 *            : Action of all probabilistic transitions in probTransitionsA and probTransitionsB
	 * @param probTransitionsA
	 *            : All probabilistic transitions of effect class A
	 * @param probTransitionsB
	 *            : All probabilistic transitions of effect class B
	 * @return All probabilistic transitions of a merged effect class A and B
	 * @throws XMDPException
	 */
	private Set<ProbabilisticTransition<IAction>> merge(IAction action,
			Set<ProbabilisticTransition<IAction>> probTransitionsA,
			Set<ProbabilisticTransition<IAction>> probTransitionsB) throws XMDPException {
		Set<ProbabilisticTransition<IAction>> mergedProbTransitions = new HashSet<>();

		for (ProbabilisticTransition<IAction> probTransA : probTransitionsA) {
			if (!action.equals(probTransA.getAction())) {
				throw new IncompatibleActionException(probTransA.getAction());
			}

			Discriminant discrA = probTransA.getDiscriminant();
			ProbabilisticEffect probEffectA = probTransA.getProbabilisticEffect();

			for (ProbabilisticTransition<IAction> probTransB : probTransitionsB) {
				if (!action.equals(probTransB.getAction())) {
					throw new IncompatibleActionException(probTransB.getAction());
				}

				Discriminant discrB = probTransB.getDiscriminant();
				ProbabilisticEffect probEffectB = probTransB.getProbabilisticEffect();

				DiscriminantClass aggrDiscrClass = new DiscriminantClass();
				aggrDiscrClass.addAll(discrA.getDiscriminantClass());
				aggrDiscrClass.addAll(discrB.getDiscriminantClass());
				Discriminant aggrDiscr = new Discriminant(aggrDiscrClass);
				aggrDiscr.addAll(discrA);
				aggrDiscr.addAll(discrB);

				EffectClass aggrEffectClass = new EffectClass();
				aggrEffectClass.addAll(probEffectA.getEffectClass());
				aggrEffectClass.addAll(probEffectB.getEffectClass());
				ProbabilisticEffect aggrProbEffect = new ProbabilisticEffect(aggrEffectClass);
				aggrProbEffect.putAll(probEffectA, probEffectB);

				ProbabilisticTransition<IAction> mergedProbTrans = new ProbabilisticTransition<>(aggrProbEffect,
						aggrDiscr, action);
				mergedProbTransitions.add(mergedProbTrans);
			}
		}
		return mergedProbTransitions;
	}

	/**
	 * {@link PartialModuleCommandsBuilder} is an interface of a function that builds a set of (partial) commands of a
	 * module, that update the effect class of a given action description.
	 * 
	 * @author rsukkerd
	 *
	 */
	interface PartialModuleCommandsBuilder {

		/**
		 * Build partial commands of a module.
		 * 
		 * @param actionDescription
		 *            : Action description of an effect class (possibly merged if there are multiple action types whose
		 *            effect classes intersect)
		 * @return Commands for updating the effect class of actionDescription
		 * @throws XMDPException
		 */
		String buildPartialModuleCommands(IActionDescription<IAction> actionDescription) throws XMDPException;
	}

	/**
	 * {@link ActionFilter} is an interface to a function that filters actions of the helper module and any reward
	 * structure. In the case of DTMC, this function is to remove actions that are not present its corresponding policy
	 * from the helper module and from any reward structure.
	 * 
	 * @author rsukkerd
	 *
	 */
	interface ActionFilter {

		/**
		 * Filter actions that are present in the model.
		 * 
		 * @param action
		 * @return True iff action is present in the model
		 */
		boolean filterAction(IAction action);
	}

}
