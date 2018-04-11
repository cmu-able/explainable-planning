package prismconnector;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarBoolean;
import factors.IStateVarDouble;
import factors.IStateVarInt;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.Effect;
import mdp.EffectClass;
import mdp.IActionDescription;
import mdp.IFactoredPSO;
import mdp.Precondition;
import mdp.ProbabilisticEffect;
import mdp.XMDP;
import metrics.IQFunction;
import metrics.Transition;
import metrics.TransitionDefinition;
import preferences.CostFunction;
import preferences.ILinearCostFunction;

public class PrismMDPTranslator {

	private static final String INDENT = "  ";
	private static final String SRC_SUFFIX = "Src";

	private XMDP mXMDP;
	private ValueEncodingScheme mEncodings;

	private boolean mThreeParamRewards;

	public PrismMDPTranslator(XMDP xmdp, boolean threeParamRewards) {
		mXMDP = xmdp;
		mEncodings = new ValueEncodingScheme(mXMDP.getStateVarDefs(), mXMDP.getActionDefs());
		mThreeParamRewards = threeParamRewards;
	}

	public String getMDPTranslation() throws VarNotFoundException, EffectClassNotFoundException,
			AttributeNameNotFoundException, IncompatibleVarException, DiscriminantNotFoundException {
		String constsDecl = buildConstsDecl(mXMDP.getStateVarDefs());
		String modules = buildModules();
		String rewards = buildRewards();
		StringBuilder builder = new StringBuilder();
		builder.append("mdp");
		builder.append("\n\n");
		builder.append(constsDecl);
		builder.append("\n");
		builder.append(modules);
		builder.append("\n");
		builder.append(rewards);
		return builder.toString();
	}

	/**
	 * 
	 * @return P>=1 [ F "{varName}={encoded int value} & ..." ]
	 */
	public String getGoalPropertyTranslation() {
		Set<StateVar<IStateVarValue>> goalVars = mXMDP.getGoal();
		StringBuilder builder = new StringBuilder();
		builder.append("P>=1 [ F \"");
		boolean firstVar = true;
		for (StateVar<IStateVarValue> goalVar : goalVars) {
			Integer encodedValue = mEncodings.getEncodedIntValue(goalVar.getDefinition(), goalVar.getValue());
			if (!firstVar) {
				builder.append(" & ");
			} else {
				firstVar = false;
			}
			builder.append(goalVar.getName());
			builder.append("=");
			builder.append(encodedValue);
		}
		builder.append("\" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param stateVarDefs
	 *            Definitions of all state variables
	 * @return const int {varName}_{value} = {encoded int value}; ...
	 */
	private String buildConstsDecl(Set<StateVarDefinition<IStateVarValue>> stateVarDefs) {
		StringBuilder builder = new StringBuilder();
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateVarDefs) {
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
				builder.append(value);
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
	 * @param stateVarDef
	 * @return formula {varName}Double = ({varName}={encoded int}) ? {double value} : ... : -1;
	 */
	private String buildVarDoubleFormula(StateVarDefinition<IStateVarDouble> stateVarDef) {
		StringBuilder builder = new StringBuilder();
		String formulaName = stateVarDef.getName() + "Double";
		builder.append("formula ");
		builder.append(formulaName);
		builder.append(" = ");
		for (IStateVarDouble value : stateVarDef.getPossibleValues()) {
			Integer encodedInt = mEncodings.getEncodedIntValue(stateVarDef, value);
			builder.append("(");
			builder.append(stateVarDef.getName());
			builder.append("=");
			builder.append(encodedInt);
			builder.append(") ? ");
			builder.append(value.getValue());
			builder.append(" : ");
		}
		builder.append("-1;");
		return builder.toString();
	}

	/**
	 * 
	 * @return module {name} {vars decl} {commands} endmodule ...
	 * @throws VarNotFoundException
	 * @throws EffectClassNotFoundException
	 */
	private String buildModules() throws VarNotFoundException, EffectClassNotFoundException {
		Set<EffectClass> allEffectClasses = new HashSet<>();
		for (ActionDefinition<IAction> actionDef : mXMDP.getActionDefs()) {
			for (IAction action : actionDef.getActions()) {
				IFactoredPSO actionPSO = mXMDP.getTransitionFunction(action);
				Set<EffectClass> actionEffectClasses = actionPSO.getIndependentEffectClasses();
				allEffectClasses.addAll(actionEffectClasses);
			}
		}

		StringBuilder builder = new StringBuilder();
		int moduleCount = 0;

		while (!allEffectClasses.isEmpty()) {
			moduleCount++;
			Iterator<EffectClass> iter = allEffectClasses.iterator();
			EffectClass effectClass = iter.next();
			IAction action = effectClass.getAction();
			iter.remove();

			Set<StateVarDefinition<IStateVarValue>> moduleVarDefs = new HashSet<>();
			Map<IAction, EffectClass> overlappingEffectActions = new HashMap<>();
			moduleVarDefs.addAll(effectClass.getAllVarDefs());
			overlappingEffectActions.put(action, effectClass);

			while (iter.hasNext()) {
				EffectClass nextEffectClass = iter.next();
				IAction nextAction = nextEffectClass.getAction();

				if (!action.equals(nextAction) && effectClass.overlaps(nextEffectClass)) {
					moduleVarDefs.addAll(nextEffectClass.getAllVarDefs());
					overlappingEffectActions.put(nextAction, nextEffectClass);
					iter.remove();
				}
			}

			String module = buildModule("module_" + moduleCount, moduleVarDefs, overlappingEffectActions);
			builder.append(module);
			builder.append("\n\n");
		}

		if (mThreeParamRewards) {
			String helperModule = buildHelperModule();
			builder.append(helperModule);
			builder.append("\n\n");
		}

		return builder.toString();
	}

	/**
	 * 
	 * @param moduleName
	 *            A unique name of the module
	 * @param moduleVarDefs
	 *            Variables of the module
	 * @param overlappingEffectActions
	 *            A mapping from each action to its (one) effect class that overlaps with those of other actions in the
	 *            map
	 * @return module {name} {vars decl} {commands} endmodule
	 * @throws VarNotFoundException
	 * @throws EffectClassNotFoundException
	 */
	private String buildModule(String moduleName, Set<StateVarDefinition<IStateVarValue>> moduleVarDefs,
			Map<IAction, EffectClass> overlappingEffectActions)
			throws VarNotFoundException, EffectClassNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("module ");
		builder.append(moduleName);
		builder.append("\n");
		String varsDecl = buildModuleVarsDecl(moduleVarDefs);
		builder.append(varsDecl);

		if (mThreeParamRewards) {
			builder.append(INDENT);
			String moduleSyncVarDecl = buildModuleSyncVarDecl(moduleName);
			builder.append(moduleSyncVarDecl);
			builder.append("\n");
		}

		builder.append("\n");
		String commands = buildModuleCommands(overlappingEffectActions);
		builder.append(commands);

		if (mThreeParamRewards) {
			builder.append(INDENT);
			String moduleSyncCommand = buildModuleSyncCommand(moduleName);
			builder.append(moduleSyncCommand);
			builder.append("\n");
		}

		builder.append("endmodule");
		return builder.toString();
	}

	/**
	 * 
	 * @param moduleName
	 * @return {moduleName}_go : bool init true;
	 */
	private String buildModuleSyncVarDecl(String moduleName) {
		return moduleName + "_go : bool init true;";
	}

	/**
	 * 
	 * @param moduleName
	 * @return [next] !{moduleName}_go -> ({moduleName}_go'=true);
	 */
	private String buildModuleSyncCommand(String moduleName) {
		return "[next] !" + moduleName + "_go -> (" + moduleName + "_go'=true);";
	}

	/**
	 * 
	 * @param moduleVarDefs
	 * @return {varName} : [0..{maximum encoded int}] init {encoded int initial value}; ...
	 * @throws VarNotFoundException
	 */
	private String buildModuleVarsDecl(Set<StateVarDefinition<IStateVarValue>> moduleVarDefs)
			throws VarNotFoundException {
		return buildModuleVarsDecl(moduleVarDefs, "");
	}

	/**
	 * 
	 * @param moduleVarDefs
	 *            Variables of the module
	 * @param nameSuffix
	 *            Suffix for each variable's name
	 * @return {varName{Suffix}} : [0..{maximum encoded int}] init {encoded int initial value}; ...
	 * @throws VarNotFoundException
	 */
	private String buildModuleVarsDecl(Set<StateVarDefinition<IStateVarValue>> moduleVarDefs, String nameSuffix)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		for (StateVarDefinition<IStateVarValue> stateVarDef : moduleVarDefs) {
			StateVar<IStateVarValue> iniStateVar = mXMDP.getInitialStateVar(stateVarDef);
			IStateVarValue iniValue = iniStateVar.getValue();
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
	 */
	private String buildModuleVarDecl(StateVarDefinition<IStateVarValue> varDef, String nameSuffix,
			IStateVarValue iniValue) {
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
	 * @param boolVarDef
	 * @param nameSuffix
	 * @param iniValBoolean
	 * @return {varName{Suffix}} : bool init {initial value};
	 */
	private String buildBooleanModuleVarDecl(StateVarDefinition<IStateVarBoolean> boolVarDef, String nameSuffix,
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
	private String buildIntModuleVarDecl(StateVarDefinition<IStateVarInt> intVarDef, String nameSuffix,
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

	private <E extends IStateVarValue> StateVarDefinition<E> castTypeStateVarDef(
			StateVarDefinition<IStateVarValue> genericVarDef, Class<E> type) {
		Set<E> possibleValues = new HashSet<>();
		for (IStateVarValue value : genericVarDef.getPossibleValues()) {
			possibleValues.add(type.cast(value));
		}
		return new StateVarDefinition<>(genericVarDef.getName(), possibleValues);
	}

	/**
	 * 
	 * @param overlappingEffectActions
	 *            A mapping from each action to its (one) effect class that overlaps with those of other actions in the
	 *            map
	 * @return [actionX] {guard_1} -> {updates_1}; ... [actionZ] {guard_p} -> {updates_p};
	 * @throws EffectClassNotFoundException
	 */
	private String buildModuleCommands(Map<IAction, EffectClass> overlappingEffectActions)
			throws EffectClassNotFoundException {
		StringBuilder builder = new StringBuilder();
		for (Entry<IAction, EffectClass> entry : overlappingEffectActions.entrySet()) {
			IAction action = entry.getKey();
			EffectClass effectClass = entry.getValue();
			IFactoredPSO actionPSO = mXMDP.getTransitionFunction(action);
			IActionDescription actionDesc = actionPSO.getActionDescription(effectClass);
			for (Entry<Discriminant, ProbabilisticEffect> e : actionDesc) {
				Discriminant discriminant = e.getKey();
				ProbabilisticEffect probEffect = e.getValue();
				String command = buildModuleCommand(action, discriminant, probEffect);
				builder.append(INDENT);
				builder.append(command);
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param action
	 * @param context
	 * @param effects
	 * @return [actionName] {guard} -> {updates};
	 */
	private String buildModuleCommand(IAction action, Discriminant discriminant, ProbabilisticEffect probEffect) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(action.getName());
		builder.append("] ");
		builder.append(buildGuard(discriminant));
		builder.append(" -> ");
		builder.append(buildUpdates(probEffect));
		builder.append(";");
		return builder.toString();
	}

	/**
	 * 
	 * @param discriminant
	 * @return {varName_1}={encoded int value} & ... & {varName_m}={encoded int value}
	 */
	private String buildGuard(Discriminant discriminant) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (StateVar<IStateVarValue> var : discriminant) {
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
	 * @param effects
	 * @return {prob_1}:{update_1} + ... + {prob_k}:{update_k}
	 */
	private String buildUpdates(ProbabilisticEffect probEffect) {
		StringBuilder builder = new StringBuilder();
		boolean firstBranch = true;
		for (Entry<Effect, Double> entry : probEffect) {
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
	 * @param effect
	 * @return {var_1 update}&...&{var_n update}
	 */
	private String buildUpdate(Effect effect) {
		StringBuilder builder = new StringBuilder();
		boolean firstVar = true;
		for (StateVar<IStateVarValue> stateVar : effect) {
			String update = buildVariableUpdate(stateVar);
			if (!firstVar) {
				builder.append("&");
			} else {
				firstVar = false;
			}
			builder.append(update);
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param updatedStateVar
	 * @return ({varName}'={encoded int value})
	 */
	private String buildVariableUpdate(StateVar<IStateVarValue> updatedStateVar) {
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
	 * @return A helper module that copies values of the variables in the source state when an action is taken, and
	 *         saves the value of that action
	 * @throws VarNotFoundException
	 */
	private String buildHelperModule() throws VarNotFoundException {
		String srcVarsDecl = buildModuleVarsDecl(mXMDP.getStateVarDefs(), SRC_SUFFIX);
		String actionsDecl = buildHelperActionsDecl(mXMDP.getActionDefs());
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
		String copyCmds = buildHelperCopyCommands(mXMDP.getActionDefs(), SRC_SUFFIX);
		builder.append(copyCmds);
		builder.append(INDENT);
		builder.append(nextCmd);
		builder.append("\n");
		builder.append("endmodule");
		return builder.toString();
	}

	/**
	 * 
	 * @param actionDefs
	 * @return {actionTypeName} : [0..{maximum encoded int}] init 0; ...
	 */
	private String buildHelperActionsDecl(Set<ActionDefinition<IAction>> actionDefs) {
		StringBuilder builder = new StringBuilder();
		for (ActionDefinition<IAction> actionDef : actionDefs) {
			Integer maxEncodedValue = mEncodings.getMaximumEncodedIntValue(actionDef);
			builder.append(INDENT);
			builder.append(actionDef.getName());
			builder.append(" : [0..");
			builder.append(maxEncodedValue);
			builder.append("] init 0;");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param actionDefs
	 * @param nameSuffix
	 * @return [{actionName}] readyToCopy -> ({varName{Suffix}}'={varName}) & ... & ({actionTypeName}'={encoded action
	 *         value}) & (readyToCopy'=false); ...
	 */
	private String buildHelperCopyCommands(Set<ActionDefinition<IAction>> actionDefs, String nameSuffix) {
		StringBuilder builder = new StringBuilder();
		for (ActionDefinition<IAction> actionDef : actionDefs) {
			for (IAction action : actionDef.getActions()) {
				builder.append(INDENT);
				builder.append("[");
				builder.append(action.getName());
				builder.append("]");
				builder.append(" readyToCopy -> ");
				IFactoredPSO actionPSO = mXMDP.getTransitionFunction(action);
				Set<EffectClass> effectClasses = actionPSO.getIndependentEffectClasses();
				boolean firstClass = true;
				for (EffectClass effectClass : effectClasses) {
					String effectCopyUpdate = buildEffectCopyUpdate(effectClass, nameSuffix);
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
	private String buildEffectCopyUpdate(EffectClass effectClass, String nameSuffix) {
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
	 * @return formula computeCost = !readyToCopy; rewards "cost" ... endrewards
	 * @throws VarNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 */
	private String buildRewards() throws VarNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("formula computeCost = !readyToCopy;");
		builder.append("\n\n");
		builder.append("rewards \"cost\"");
		Set<IQFunction> qFunctions = mXMDP.getQFunctions();
		for (IQFunction qFunction : qFunctions) {
			String rewardItems = buildRewardItems(qFunction);
			builder.append(rewardItems);
		}
		builder.append("endrewards");
		return builder.toString();
	}

	/**
	 * Build reward items for a given QA function.
	 * 
	 * @param qFunction
	 * @param linearCostFunc
	 * @param scalingConst
	 * @return computeCost & {actionTypeName}={encoded action value} & {srcVarName}={value} ... & {destVarName}={value}
	 *         ... : {scaled cost}; ...
	 * @throws VarNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 */
	private String buildRewardItems(IQFunction qFunction) throws VarNotFoundException, AttributeNameNotFoundException,
			IncompatibleVarException, DiscriminantNotFoundException {
		TransitionDefinition transDef = qFunction.getTransitionDefinition();
		Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs = transDef.getSrcStateVarDefs();
		Set<StateVarDefinition<IStateVarValue>> destStateVarDefs = transDef.getDestStateVarDefs();
		ActionDefinition<IAction> actionDef = transDef.getActionDef();

		StringBuilder builder = new StringBuilder();
		String actionTypeName = actionDef.getName();

		for (IAction action : actionDef.getActions()) {
			Integer encodedActionValue = mEncodings.getEncodedIntValue(actionDef, action);

			Set<Set<StateVar<IStateVarValue>>> srcCombinations = getApplicableSrcValuesCombinations(action,
					srcStateVarDefs);
			for (Set<StateVar<IStateVarValue>> srcVars : srcCombinations) {
				String srcPartialGuard = buildPartialGuard(srcVars, SRC_SUFFIX);

				Set<Set<StateVar<IStateVarValue>>> destCombinations = getPossibleDestValuesCombination(action,
						destStateVarDefs, srcVars);
				for (Set<StateVar<IStateVarValue>> destVars : destCombinations) {
					String destPartialGuard = buildPartialGuard(destVars);

					Transition trans = new Transition(action, srcVars, destVars);
					double qValue = qFunction.getValue(trans);
					CostFunction costFunc = mXMDP.getCostFunction();
					ILinearCostFunction linearCostFunc = costFunc.getLinearCostFunction(qFunction);
					double scalingConst = costFunc.getScalingConstant(qFunction);
					double cost = linearCostFunc.getCost(qValue);
					double scaledCost = scalingConst * cost;

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
					builder.append(scaledCost);
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
	 */
	private String buildPartialGuard(Set<StateVar<IStateVarValue>> stateVars) {
		return buildPartialGuard(stateVars, "");
	}

	/**
	 * 
	 * @param stateVars
	 * @param nameSuffix
	 * @return {varName_1{Suffix}}={encoded int value} & ... & {varName_m{Suffix}}={encoded int value}
	 */
	private String buildPartialGuard(Set<StateVar<IStateVarValue>> stateVars, String nameSuffix) {
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

	private Set<Set<StateVar<IStateVarValue>>> getApplicableSrcValuesCombinations(IAction action,
			Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs) {
		IFactoredPSO actionPSO = mXMDP.getTransitionFunction(action);
		Precondition precond = actionPSO.getPrecondition();
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> srcVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> srcVarDef : srcStateVarDefs) {
			Set<IStateVarValue> applicableVals = precond.getApplicableValues(srcVarDef);
			srcVarValues.put(srcVarDef, applicableVals);
		}
		return getCombinations(srcVarValues);
	}

	private Set<Set<StateVar<IStateVarValue>>> getPossibleDestValuesCombination(IAction action,
			Set<StateVarDefinition<IStateVarValue>> destStateVarDefs, Set<StateVar<IStateVarValue>> srcVars)
			throws IncompatibleVarException, VarNotFoundException, DiscriminantNotFoundException {
		IFactoredPSO actionPSO = mXMDP.getTransitionFunction(action);
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> destVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> destVarDef : destStateVarDefs) {
			Discriminant discriminant = getDiscriminant(destVarDef, srcVars, actionPSO);
			Set<IStateVarValue> possibleDestVals = actionPSO.getPossibleImpact(destVarDef, discriminant);
			destVarValues.put(destVarDef, possibleDestVals);
		}
		return getCombinations(destVarValues);
	}

	private Discriminant getDiscriminant(StateVarDefinition<IStateVarValue> destVarDef,
			Set<StateVar<IStateVarValue>> srcVars, IFactoredPSO actionPSO)
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

}
