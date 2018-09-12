package solver.prismconnector;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import language.exceptions.IncompatibleActionException;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
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
import language.qfactors.ActionDefinition;
import language.qfactors.IAction;
import language.qfactors.IStateVarBoolean;
import language.qfactors.IStateVarInt;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;
import language.qfactors.StateVarDefinition;

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
	 * Build a helper module that handles cycles of choosing action, reward computation, checking if the goal is reached
	 * for termination.
	 * 
	 * @param actionPSOs
	 * @param helperActionFilter
	 * @return A helper module that handles cycles of choosing action, reward computation, checking if the goal is
	 *         reached for termination
	 */
	String buildHelperModule(Iterable<FactoredPSO<IAction>> actionPSOs, HelperModuleActionFilter helperActionFilter) {
		StringBuilder builder = new StringBuilder();
		builder.append("module helper");
		builder.append("\n");
		builder.append(PrismTranslatorUtils.INDENT);
		builder.append("barrier : bool init false;");
		builder.append("\n");
		builder.append(PrismTranslatorUtils.INDENT);
		builder.append("computeGo : bool init false;");
		builder.append("\n\n");

		for (FactoredPSO<IAction> actionPSO : actionPSOs) {
			ActionDefinition<IAction> actionDef = actionPSO.getActionDefinition();

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
				builder.append(" !computeGo & !barrier -> (computeGo'=true) & (barrier'=true);");
				builder.append("\n");
			}
		}

		builder.append("\n");
		builder.append(PrismTranslatorUtils.INDENT);
		builder.append("[compute] computeGo & barrier -> (computeGo'=false);");
		builder.append("\n");
		builder.append(PrismTranslatorUtils.INDENT);
		builder.append("[next] !computeGo & barrier & !goal -> (barrier'=false);");
		builder.append("\n");
		builder.append(PrismTranslatorUtils.INDENT);
		builder.append("[end] !computeGo & barrier & goal -> true;");
		builder.append("\n");
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
		IStateVarInt lowerBound = Collections.max(intVarDef.getPossibleValues());
		IStateVarInt uppberBound = Collections.max(intVarDef.getPossibleValues());
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
	 * @param helperActionFilter
	 *            : A function that filters actions of the helper module
	 * @return module {name} {vars decl} {commands} endmodule ...
	 * @throws XMDPException
	 */
	String buildModules(StateSpace stateSpace, StateVarTuple iniState, Iterable<ActionDefinition<IAction>> actionDefs,
			Iterable<FactoredPSO<IAction>> actionPSOs, PartialModuleCommandsBuilder partialCommandsBuilder,
			HelperModuleActionFilter helperActionFilter) throws XMDPException {
		// This determines a set of module variables. Each set of variables are updated independently.
		// These variables are updated by some actions in the model.
		Set<Map<EffectClass, FactoredPSO<IAction>>> chainsOfEffectClasses = getChainsOfEffectClasses(actionPSOs);

		// These variables are unmodified by the actions in the model.
		// This is mostly for handling DTMC.
		StateSpace unmodifiedVarSpace = stateSpace;

		StringBuilder builder = new StringBuilder();
		int moduleCount = 0;
		boolean first = true;

		for (Map<EffectClass, FactoredPSO<IAction>> chain : chainsOfEffectClasses) {
			moduleCount++;
			StateSpace moduleVarSpace = new StateSpace();
			Map<FactoredPSO<IAction>, Set<EffectClass>> moduleActionPSOs = new HashMap<>();

			for (Entry<EffectClass, FactoredPSO<IAction>> entry : chain.entrySet()) {
				EffectClass effectClass = entry.getKey();
				FactoredPSO<IAction> actionPSO = entry.getValue();

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

		String helperModule = buildHelperModule(actionPSOs, helperActionFilter);
		builder.append("\n\n");
		builder.append(helperModule);
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
	 *         effect classes are "chained" iff: (1)~they are associated with different action types, but they have
	 *         overlapping state variables, or (2)~they are associated with the same action type (they do not overlap),
	 *         but they overlap with other effect classes of other action types that are "chained".
	 */
	Set<Map<EffectClass, FactoredPSO<IAction>>> getChainsOfEffectClasses(Iterable<FactoredPSO<IAction>> actionPSOs) {
		Set<Map<EffectClass, FactoredPSO<IAction>>> currChains = new HashSet<>();
		boolean firstPSO = true;
		for (FactoredPSO<IAction> actionPSO : actionPSOs) {
			Set<EffectClass> actionEffectClasses = actionPSO.getIndependentEffectClasses();
			if (firstPSO) {
				for (EffectClass effectClass : actionEffectClasses) {
					Map<EffectClass, FactoredPSO<IAction>> iniChain = new HashMap<>();
					iniChain.put(effectClass, actionPSO);
					currChains.add(iniChain);
				}
				firstPSO = false;
				continue;
			}
			for (EffectClass effectClass : actionEffectClasses) {
				currChains = getChainsOfEffectClassesHelper(currChains, effectClass, actionPSO);
			}
		}
		return currChains;
	}

	private Set<Map<EffectClass, FactoredPSO<IAction>>> getChainsOfEffectClassesHelper(
			Set<Map<EffectClass, FactoredPSO<IAction>>> currChains, EffectClass effectClass,
			FactoredPSO<IAction> actionPSO) {
		Set<Map<EffectClass, FactoredPSO<IAction>>> res = new HashSet<>();
		Map<EffectClass, FactoredPSO<IAction>> newChain = new HashMap<>();
		for (Map<EffectClass, FactoredPSO<IAction>> chain : currChains) {
			boolean overlapped = false;
			for (EffectClass currEffectClass : chain.keySet()) {
				if (effectClass.overlaps(currEffectClass)) {
					newChain.putAll(chain);
					overlapped = true;
					break;
				}
			}
			if (!overlapped) {
				res.add(chain);
			}
		}
		newChain.put(effectClass, actionPSO);
		res.add(newChain);
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
	 * {@link HelperModuleActionFilter} is an interface to a function that filters actions of the helper module. In the
	 * case of DTMC, this function is to remove actions that are not present its corresponding policy from the helper
	 * module.
	 * 
	 * @author rsukkerd
	 *
	 */
	interface HelperModuleActionFilter {

		/**
		 * Filter actions that are present in the model.
		 * 
		 * @param action
		 * @return True iff action is present in the model
		 */
		boolean filterAction(IAction action);
	}

}
