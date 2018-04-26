package prismconnector;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import dtmc.TwoTBN;
import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarBoolean;
import factors.IStateVarInt;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.ActionSpace;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.Effect;
import mdp.EffectClass;
import mdp.FactoredPSO;
import mdp.IActionDescription;
import mdp.IPredicate;
import mdp.Precondition;
import mdp.ProbabilisticEffect;
import mdp.ProbabilisticTransition;
import mdp.State;
import mdp.StateSpace;
import mdp.TransitionFunction;
import metrics.IQFunction;
import metrics.Transition;
import metrics.TransitionDefinition;
import policy.Predicate;
import preferences.AttributeCostFunction;
import preferences.CostFunction;

public class PrismTranslatorUtilities {

	private static final String INDENT = "  ";
	private static final String SRC_SUFFIX = "Src";

	private ValueEncodingScheme mEncodings;
	private boolean mThreeParamRewards;

	private String currModuleName; // Book-keeping

	public PrismTranslatorUtilities(ValueEncodingScheme encodings, boolean threeParamRewards) {
		mEncodings = encodings;
		mThreeParamRewards = threeParamRewards;
	}

	public ValueEncodingScheme getValueEncodingScheme() {
		return mEncodings;
	}

	void setCurrentModuleName(String moduleName) {
		currModuleName = moduleName;
	}

	/**
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
					continue;
				}
				Integer encodedValue = mEncodings.getEncodedIntValue(stateVarDef, value);
				builder.append("const int ");
				builder.append(varName);
				builder.append("_");
				String valueString = String.valueOf(value);
				builder.append(sanitizeNameString(valueString));
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
	 * @param stateSpace
	 * @param iniState
	 * @param actionSpace
	 * @param transFunction
	 * @return A helper module that copies values of the variables in the source state when an action is taken, and
	 *         saves the value of that action
	 * @throws VarNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 */
	String buildHelperModule(StateSpace stateSpace, State iniState, ActionSpace actionSpace,
			TransitionFunction transFunction) throws VarNotFoundException, ActionDefinitionNotFoundException {
		String srcVarsDecl = buildModuleVarsDecl(stateSpace, iniState, SRC_SUFFIX);
		String actionsDecl = buildHelperActionsDecl(actionSpace);
		String readyToCopyDecl = "readyToCopy : bool init true;";
		String nextCmd = "[next] !readyToCopy -> (readyToCopy'=true);";

		StringBuilder builder = new StringBuilder();
		builder.append("module helper");
		builder.append("\n");
		builder.append(srcVarsDecl);
		builder.append(actionsDecl);
		builder.append(INDENT);
		builder.append(readyToCopyDecl);
		builder.append("\n\n");
		String copyCmds = buildHelperCopyCommands(transFunction, SRC_SUFFIX);
		builder.append(copyCmds);
		builder.append("\n");
		builder.append(INDENT);
		builder.append(nextCmd);
		builder.append("\n");
		builder.append("endmodule");
		return builder.toString();
	}

	/**
	 * 
	 * @param actionSpace
	 * @return {actionTypeName} : [0..{maximum encoded int}] init 0; ...
	 * @throws ActionDefinitionNotFoundException
	 */
	String buildHelperActionsDecl(ActionSpace actionSpace) throws ActionDefinitionNotFoundException {
		StringBuilder builder = new StringBuilder();
		for (ActionDefinition<IAction> actionDef : actionSpace) {
			Integer maxEncodedValue = mEncodings.getMaximumEncodedIntValue(actionDef);
			builder.append(INDENT);
			builder.append(actionDef.getName());
			builder.append(" : [0..");
			builder.append(maxEncodedValue);
			builder.append("] init 0;");
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param transFunction
	 * @param nameSuffix
	 * @return [{actionName}] readyToCopy -> ({varName{Suffix}}'={varName}) & ... & ({actionTypeName}'={encoded action
	 *         value}) & (readyToCopy'=false); ...
	 * @throws ActionDefinitionNotFoundException
	 */
	String buildHelperCopyCommands(TransitionFunction transFunction, String nameSuffix)
			throws ActionDefinitionNotFoundException {
		StringBuilder builder = new StringBuilder();
		for (FactoredPSO<IAction> actionPSO : transFunction) {
			ActionDefinition<IAction> actionDef = actionPSO.getActionDefinition();
			for (IAction action : actionDef.getActions()) {
				builder.append(INDENT);
				builder.append("[");
				builder.append(sanitizeNameString(action.getName()));
				builder.append("]");
				builder.append(" readyToCopy -> ");

				Set<EffectClass> effectClasses = actionPSO.getIndependentEffectClasses();
				boolean firstClass = true;
				for (EffectClass effectClass : effectClasses) {
					String effectCopyUpdate = buildPreEffectCopyUpdate(effectClass, nameSuffix);
					if (!firstClass) {
						builder.append(" & ");
					} else {
						firstClass = false;
					}
					builder.append(effectCopyUpdate);
				}
				builder.append(" & ");
				String actionCopyUpdate = "(" + actionDef.getName() + "'="
						+ mEncodings.getEncodedIntValue(actionDef, action) + ")";
				builder.append(actionCopyUpdate);
				builder.append(" & (readyToCopy'=false);");
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param effectClass
	 * @param nameSuffix
	 * @return ({varName{Suffix}}'={varName}) & ...
	 */
	String buildPreEffectCopyUpdate(EffectClass effectClass, String nameSuffix) {
		StringBuilder builder = new StringBuilder();
		boolean firstVar = true;
		for (StateVarDefinition<IStateVarValue> varDef : effectClass) {
			if (!firstVar) {
				builder.append(" & ");
			} else {
				firstVar = false;
			}
			builder.append("(");
			builder.append(varDef.getName());
			builder.append(nameSuffix);
			builder.append("'");
			builder.append("=");
			builder.append(varDef.getName());
			builder.append(")");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param moduleVarSpace
	 *            Variables of the module
	 * @param
	 * @return {varName} : [0..{maximum encoded int}] init {encoded int initial value}; ...
	 * @throws VarNotFoundException
	 */
	String buildModuleVarsDecl(StateSpace moduleVarSpace, State iniState) throws VarNotFoundException {
		return buildModuleVarsDecl(moduleVarSpace, iniState, "");
	}

	/**
	 * 
	 * @param moduleVarSpace
	 *            Variables of the module
	 * @param iniState
	 *            Initial state
	 * @param nameSuffix
	 *            Suffix for each variable's name
	 * @return {varName{Suffix}} : [0..{maximum encoded int}] init {encoded int initial value}; ...
	 * @throws VarNotFoundException
	 */
	String buildModuleVarsDecl(StateSpace moduleVarSpace, State iniState, String nameSuffix)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		for (StateVarDefinition<IStateVarValue> stateVarDef : moduleVarSpace) {
			IStateVarValue iniValue = iniState.getStateVarValue(IStateVarValue.class, stateVarDef);
			String varDecl;
			if (iniValue instanceof IStateVarBoolean) {
				StateVarDefinition<IStateVarBoolean> boolVarDef = castTypeStateVarDef(stateVarDef,
						IStateVarBoolean.class);
				varDecl = buildBooleanModuleVarDecl(boolVarDef, nameSuffix, (IStateVarBoolean) iniValue);
			} else if (iniValue instanceof IStateVarInt) {
				StateVarDefinition<IStateVarInt> intVarDef = castTypeStateVarDef(stateVarDef, IStateVarInt.class);
				varDecl = buildIntModuleVarDecl(intVarDef, nameSuffix, (IStateVarInt) iniValue);
			} else {
				varDecl = buildModuleVarDecl(stateVarDef, nameSuffix, iniValue);
			}
			builder.append(INDENT);
			builder.append(varDecl);
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param varDef
	 * @param nameSuffix
	 * @param iniValue
	 * @return {varName{Suffix}} : [0..{maximum encoded int}] init {encoded int initial value};
	 * @throws VarNotFoundException
	 */
	String buildModuleVarDecl(StateVarDefinition<IStateVarValue> varDef, String nameSuffix, IStateVarValue iniValue)
			throws VarNotFoundException {
		Integer maxEncodedValue = mEncodings.getMaximumEncodedIntValue(varDef);
		Integer encodedIniValue = mEncodings.getEncodedIntValue(varDef, iniValue);
		StringBuilder builder = new StringBuilder();
		builder.append(varDef.getName());
		builder.append(nameSuffix);
		builder.append(" : [0..");
		builder.append(maxEncodedValue);
		builder.append("] init ");
		builder.append(encodedIniValue);
		builder.append(";");
		return builder.toString();
	}

	/**
	 * 
	 * @param moduleName
	 * @return {moduleName}_go : bool init true;
	 */
	String buildModuleSyncVarDecl(String moduleName) {
		return moduleName + "_go : bool init true;";
	}

	/**
	 * 
	 * @param moduleName
	 * @return [next] !{moduleName}_go -> ({moduleName}_go'=true);
	 */
	String buildModuleSyncCommand(String moduleName) {
		return "[next] !" + moduleName + "_go -> (" + moduleName + "_go'=true);";
	}

	/**
	 * 
	 * @param moduleName
	 * @return [] {moduleName}_go -> ({moduleName}_go'=false);
	 */
	String buildModuleDoNothingCommand(String moduleName) {
		return "[] " + moduleName + "_go -> (" + moduleName + "_go'=false);";
	}

	/**
	 * 
	 * @param boolVarDef
	 * @param nameSuffix
	 * @param iniValBoolean
	 * @return {varName{Suffix}} : bool init {initial value};
	 */
	String buildBooleanModuleVarDecl(StateVarDefinition<IStateVarBoolean> boolVarDef, String nameSuffix,
			IStateVarBoolean iniValBoolean) {
		StringBuilder builder = new StringBuilder();
		builder.append(boolVarDef.getName());
		builder.append(nameSuffix);
		builder.append(" : bool init ");
		builder.append(iniValBoolean.getValue() ? "true" : "false");
		builder.append(";");
		return builder.toString();
	}

	/**
	 * 
	 * @param intVarDef
	 * @param nameSuffix
	 * @param iniValInt
	 * @return {varName{Suffix}} : [{min}..{max}] init {initial value};
	 */
	String buildIntModuleVarDecl(StateVarDefinition<IStateVarInt> intVarDef, String nameSuffix,
			IStateVarInt iniValInt) {
		StringBuilder builder = new StringBuilder();
		builder.append(intVarDef.getName());
		builder.append(nameSuffix);
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
	 * Build partial module commands for MDP.
	 * 
	 * @param actionDescription
	 *            Action description of an effect class (possibly merged)
	 * @return commands for updating a particular effect class of actionDescription
	 * @throws ActionNotFoundException
	 * @throws VarNotFoundException
	 */
	String buildPartialModuleCommands(IActionDescription<IAction> actionDescription)
			throws ActionNotFoundException, VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		ActionDefinition<IAction> actionDef = actionDescription.getActionDefinition();
		for (IAction action : actionDef.getActions()) {
			Set<ProbabilisticTransition<IAction>> probTransitions = actionDescription
					.getProbabilisticTransitions(action);
			for (ProbabilisticTransition<IAction> probTrans : probTransitions) {
				Discriminant discriminant = probTrans.getDiscriminant();
				ProbabilisticEffect probEffect = probTrans.getProbabilisticEffect();
				String command = buildModuleCommand(action, discriminant, probEffect);
				builder.append(INDENT);
				builder.append(command);
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	/**
	 * Build partial module commands for DTMC.
	 * 
	 * @param actionDescription
	 *            Action description of an effect class (possibly merged)
	 * @param twoTBN
	 *            2TBN of the same action type as that of actionDescription
	 * @return commands for updating a particular effect class of actionDescription
	 * @throws ActionNotFoundException
	 * @throws VarNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 */
	<E extends IAction> String buildPartialModuleCommands(IActionDescription<E> actionDescription, TwoTBN<E> twoTBN)
			throws ActionNotFoundException, VarNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException {
		StringBuilder builder = new StringBuilder();
		DiscriminantClass discrClass = actionDescription.getDiscriminantClass();
		for (Entry<Predicate, E> entry : twoTBN) {
			Predicate predicate = entry.getKey();
			E action = entry.getValue();

			Discriminant discriminant = new Discriminant(discrClass);
			for (StateVarDefinition<IStateVarValue> stateVarDef : discrClass) {
				IStateVarValue value = predicate.getStateVarValue(IStateVarValue.class, stateVarDef);
				StateVar<IStateVarValue> stateVar = new StateVar<>(stateVarDef, value);
				discriminant.add(stateVar);
			}
			ProbabilisticEffect probEffect = actionDescription.getProbabilisticEffect(discriminant, action);
			String command = buildModuleCommand(action, predicate, probEffect);
			builder.append(INDENT);
			builder.append(command);
			builder.append("\n");
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
	String buildModuleCommand(IAction action, IPredicate predicate, ProbabilisticEffect probEffect)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		String sanitizedActionName = sanitizeNameString(action.getName());
		builder.append(sanitizedActionName);
		builder.append("] ");

		if (mThreeParamRewards) {
			builder.append(currModuleName);
			builder.append("_go");
			builder.append(" & ");
		}

		String guard = buildGuard(predicate);
		String updates = buildUpdates(probEffect);
		builder.append(guard);
		builder.append(" -> ");
		builder.append(updates);
		builder.append(";");
		return builder.toString();
	}

	/**
	 * 
	 * @param predicate
	 * @return {varName_1}={encoded int value} & ... & {varName_m}={encoded int value}
	 * @throws VarNotFoundException
	 */
	String buildGuard(Iterable<StateVar<IStateVarValue>> predicate) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (StateVar<IStateVarValue> var : predicate) {
			String varName = var.getName();
			Integer encodedValue = mEncodings.getEncodedIntValue(var.getDefinition(), var.getValue());
			if (!first) {
				builder.append(" & ");
			} else {
				first = false;
			}
			builder.append(varName);
			builder.append("=");
			builder.append(encodedValue);
		}
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

			if (mThreeParamRewards) {
				builder.append("&(");
				builder.append(currModuleName);
				builder.append("_go");
				builder.append("'=false)");
			}
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param update
	 * @return {var_1 update}&...&{var_n update}
	 * @throws VarNotFoundException
	 */
	String buildUpdate(IPredicate update) throws VarNotFoundException {
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
	 * @return ({varName}'={encoded int value})
	 * @throws VarNotFoundException
	 */
	String buildVariableUpdate(StateVar<IStateVarValue> updatedStateVar) throws VarNotFoundException {
		String varName = updatedStateVar.getName();
		Integer encodedValue = mEncodings.getEncodedIntValue(updatedStateVar.getDefinition(),
				updatedStateVar.getValue());
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(varName);
		builder.append("'");
		builder.append("=");
		builder.append(encodedValue);
		builder.append(")");
		return builder.toString();
	}

	/**
	 * 
	 * @param transFunction
	 * @param qFunctions
	 * @param costFunction
	 * @return formula computeCost = !readyToCopy; rewards "cost" ... endrewards
	 * @throws VarNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws ActionNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 */
	String buildRewards(TransitionFunction transFunction, Set<IQFunction> qFunctions, CostFunction costFunction)
			throws VarNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException, ActionNotFoundException, ActionDefinitionNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("formula computeCost = !readyToCopy;");
		builder.append("\n\n");
		builder.append("rewards \"cost\"");
		builder.append("\n");
		for (IQFunction qFunction : qFunctions) {
			AttributeCostFunction<IQFunction> attrCostFunction = costFunction.getAttributeCostFunction(qFunction);
			double scalingConst = costFunction.getScalingConstant(qFunction);
			String rewardItems = buildRewardItems(transFunction, qFunction, attrCostFunction, scalingConst);
			builder.append(rewardItems);
		}
		builder.append("endrewards");
		return builder.toString();
	}

	/**
	 * Build reward items for a given QA function.
	 * 
	 * @param transFunction
	 * @param qFunction
	 * @param attrCostFunction
	 * @param scalingConst
	 * @return computeCost & {actionTypeName}={encoded action value} & {srcVarName}={value} ... & {destVarName}={value}
	 *         ... : {scaled cost}; ...
	 * @throws VarNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws ActionNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 */
	String buildRewardItems(TransitionFunction transFunction, IQFunction qFunction,
			AttributeCostFunction<IQFunction> attrCostFunction, double scalingConst)
			throws VarNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException, ActionNotFoundException, ActionDefinitionNotFoundException {
		TransitionDefinition transDef = qFunction.getTransitionDefinition();
		Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs = transDef.getSrcStateVarDefs();
		Set<StateVarDefinition<IStateVarValue>> destStateVarDefs = transDef.getDestStateVarDefs();
		ActionDefinition<IAction> actionDef = transDef.getActionDef();
		FactoredPSO<IAction> actionPSO = transFunction.getActionPSO(actionDef);

		StringBuilder builder = new StringBuilder();
		String actionTypeName = actionDef.getName();

		for (IAction action : actionDef.getActions()) {
			Integer encodedActionValue = mEncodings.getEncodedIntValue(actionDef, action);

			Set<Set<StateVar<IStateVarValue>>> srcCombinations = getApplicableSrcValuesCombinations(actionPSO, action,
					srcStateVarDefs);
			for (Set<StateVar<IStateVarValue>> srcVars : srcCombinations) {
				String srcPartialGuard = buildPartialGuard(srcVars, SRC_SUFFIX);

				Set<Set<StateVar<IStateVarValue>>> destCombinations = getPossibleDestValuesCombination(actionPSO,
						action, destStateVarDefs, srcVars);
				for (Set<StateVar<IStateVarValue>> destVars : destCombinations) {
					String destPartialGuard = buildPartialGuard(destVars);

					Transition trans = new Transition(action, srcVars, destVars);
					double attrCost = attrCostFunction.getCost(trans);
					double scaledAttrCost = scalingConst * attrCost;

					builder.append(INDENT);
					builder.append("computeCost & ");
					builder.append(actionTypeName);
					builder.append("=");
					builder.append(encodedActionValue);
					builder.append(" & ");
					builder.append(srcPartialGuard);
					builder.append(" & ");
					builder.append(destPartialGuard);
					builder.append(" : ");
					builder.append(scaledAttrCost);
					builder.append(";");
					builder.append("\n");
				}
			}
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param stateVars
	 * @return {varName_1}={encoded int value} & ... & {varName_m}={encoded int value}
	 * @throws VarNotFoundException
	 */
	private String buildPartialGuard(Set<StateVar<IStateVarValue>> stateVars) throws VarNotFoundException {
		return buildPartialGuard(stateVars, "");
	}

	/**
	 * 
	 * @param stateVars
	 * @param nameSuffix
	 * @return {varName_1{Suffix}}={encoded int value} & ... & {varName_m{Suffix}}={encoded int value}
	 * @throws VarNotFoundException
	 */
	private String buildPartialGuard(Set<StateVar<IStateVarValue>> stateVars, String nameSuffix)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (StateVar<IStateVarValue> var : stateVars) {
			String varName = var.getName();
			Integer encodedValue = mEncodings.getEncodedIntValue(var.getDefinition(), var.getValue());
			if (!first) {
				builder.append(" & ");
			} else {
				first = false;
			}
			builder.append(varName);
			builder.append(nameSuffix);
			builder.append("=");
			builder.append(encodedValue);
		}
		return builder.toString();
	}

	private Set<Set<StateVar<IStateVarValue>>> getApplicableSrcValuesCombinations(FactoredPSO<IAction> actionPSO,
			IAction action, Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs) throws ActionNotFoundException {
		Precondition precond = actionPSO.getPrecondition(action);
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> srcVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> srcVarDef : srcStateVarDefs) {
			Set<IStateVarValue> applicableVals = precond.getApplicableValues(srcVarDef);
			srcVarValues.put(srcVarDef, applicableVals);
		}
		return getCombinations(srcVarValues);
	}

	private Set<Set<StateVar<IStateVarValue>>> getPossibleDestValuesCombination(FactoredPSO<IAction> actionPSO,
			IAction action, Set<StateVarDefinition<IStateVarValue>> destStateVarDefs,
			Set<StateVar<IStateVarValue>> srcVars) throws IncompatibleVarException, VarNotFoundException,
			DiscriminantNotFoundException, ActionNotFoundException {
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> destVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> destVarDef : destStateVarDefs) {
			Discriminant discriminant = getDiscriminant(destVarDef, srcVars, actionPSO);
			Set<IStateVarValue> possibleDestVals = actionPSO.getPossibleImpact(destVarDef, discriminant, action);
			destVarValues.put(destVarDef, possibleDestVals);
		}
		return getCombinations(destVarValues);
	}

	private Discriminant getDiscriminant(StateVarDefinition<IStateVarValue> destVarDef,
			Set<StateVar<IStateVarValue>> srcVars, FactoredPSO<IAction> actionPSO)
			throws IncompatibleVarException, VarNotFoundException {
		DiscriminantClass discrClass = actionPSO.getDiscriminantClass(destVarDef);
		Discriminant discriminant = new Discriminant(discrClass);
		for (StateVar<IStateVarValue> var : srcVars) {
			if (discrClass.contains(var.getDefinition())) {
				StateVar<IStateVarValue> discrVar = new StateVar<>(var.getDefinition(), var.getValue());
				discriminant.add(discrVar);
			}
		}
		return discriminant;
	}

	private Set<Set<StateVar<IStateVarValue>>> getCombinations(
			Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> varValues) {
		Set<Set<StateVar<IStateVarValue>>> combinations = new HashSet<>();
		Set<StateVar<IStateVarValue>> emptyCombination = new HashSet<>();
		combinations.add(emptyCombination);

		// Base case: no variable
		if (varValues.isEmpty()) {
			return combinations;
		}

		StateVarDefinition<IStateVarValue> varDef = varValues.keySet().iterator().next();
		Set<IStateVarValue> values = varValues.get(varDef);

		// Base case: 1 variable
		if (varValues.size() == 1) {
			return getCombinationsHelper(varDef, values, combinations);
		}

		// Recursive case: >1 variables
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> partialVarValues = new HashMap<>(varValues);
		partialVarValues.remove(varDef);
		Set<Set<StateVar<IStateVarValue>>> partialCombinations = getCombinations(partialVarValues);
		return getCombinationsHelper(varDef, values, partialCombinations);
	}

	private Set<Set<StateVar<IStateVarValue>>> getCombinationsHelper(StateVarDefinition<IStateVarValue> varDef,
			Set<IStateVarValue> values, Set<Set<StateVar<IStateVarValue>>> partialCombinations) {
		Set<Set<StateVar<IStateVarValue>>> newCombinations = new HashSet<>();
		for (IStateVarValue value : values) {
			StateVar<IStateVarValue> newVar = new StateVar<>(varDef, value);
			for (Set<StateVar<IStateVarValue>> prevCombination : partialCombinations) {
				Set<StateVar<IStateVarValue>> newCombination = new HashSet<>();
				newCombination.addAll(prevCombination);
				newCombination.add(newVar);
				newCombinations.add(newCombination);
			}
		}
		return newCombinations;
	}

	private String sanitizeNameString(String name) {
		return name.replace(".", "_");
	}

	private <E extends IStateVarValue> StateVarDefinition<E> castTypeStateVarDef(
			StateVarDefinition<IStateVarValue> genericVarDef, Class<E> type) {
		Set<E> possibleValues = new HashSet<>();
		for (IStateVarValue value : genericVarDef.getPossibleValues()) {
			possibleValues.add(type.cast(value));
		}
		return new StateVarDefinition<>(genericVarDef.getName(), possibleValues);
	}
}
