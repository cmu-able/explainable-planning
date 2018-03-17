package prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.EffectClassNotFoundException;
import exceptions.VarNameNotFoundException;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.EffectClass;
import mdp.IActionDescription;
import mdp.IFactoredPSO;
import mdp.Policy;
import mdp.ProbabilisticEffect;
import mdp.XMDP;
import metrics.IQFunction;

public class PRISMTranslator {

	private static final String INDENT = "  ";

	private XMDP mXMDP;
	private ValueEncodingScheme mEncodings;

	public PRISMTranslator(XMDP xmdp) {
		mXMDP = xmdp;
		mEncodings = new ValueEncodingScheme(mXMDP.getStateVarDefs());
	}

	public String getMDPTranslation() throws VarNameNotFoundException, EffectClassNotFoundException {
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

	public String getGoalPropertyTranslation() {
		return null;
	}

	public String getQFunctionTranslation(IQFunction qFunction) {
		return null;
	}

	public String getDTMC(Policy policy) {
		StringBuilder builder = new StringBuilder();
		builder.append("dtmc");
		builder.append("\n");
		builder.append("module policy");
		builder.append("endmodule");
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
				Integer encodedValue = mEncodings.getEncodedIntValue(new StateVar<IStateVarValue>(varName, value));
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
	 * @return module {name} {vars decl} {commands} endmodule ...
	 * @throws VarNameNotFoundException
	 * @throws EffectClassNotFoundException
	 */
	private String buildModules() throws VarNameNotFoundException, EffectClassNotFoundException {
		Set<EffectClass> allEffectClasses = new HashSet<>();
		for (IAction action : mXMDP.getActions()) {
			IFactoredPSO actionPSO = mXMDP.getTransitionFunction(action);
			Set<EffectClass> actionEffectClasses = actionPSO.getIndependentEffectClasses();
			allEffectClasses.addAll(actionEffectClasses);
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
			moduleVarDefs.addAll(effectClass.getAffectedVarDefs());
			overlappingEffectActions.put(action, effectClass);

			while (iter.hasNext()) {
				EffectClass nextEffectClass = iter.next();
				IAction nextAction = nextEffectClass.getAction();

				if (!action.equals(nextAction) && effectClass.overlaps(nextEffectClass)) {
					moduleVarDefs.addAll(nextEffectClass.getAffectedVarDefs());
					overlappingEffectActions.put(nextAction, nextEffectClass);
					iter.remove();
				}
			}

			String module = buildModule("module_" + moduleCount, moduleVarDefs, overlappingEffectActions);
			builder.append(module);
			builder.append("\n\n");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param name
	 *            A unique name of the module
	 * @param moduleVarDefs
	 *            Variables of the module
	 * @param overlappingEffectActions
	 *            A mapping from each action to its (one) effect class that overlaps with those of other actions in the
	 *            map
	 * @return module {name} {vars decl} {commands} endmodule
	 * @throws VarNameNotFoundException
	 * @throws EffectClassNotFoundException
	 */
	private String buildModule(String name, Set<StateVarDefinition<IStateVarValue>> moduleVarDefs,
			Map<IAction, EffectClass> overlappingEffectActions)
			throws VarNameNotFoundException, EffectClassNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("module ");
		builder.append(name);
		builder.append("\n");
		String varsDecl = buildModuleVarsDecl(moduleVarDefs);
		builder.append(varsDecl);
		builder.append("\n");
		String commands = buildModuleCommands(overlappingEffectActions);
		builder.append(commands);
		builder.append("endmodule");
		return builder.toString();
	}

	/**
	 * 
	 * @param moduleVarDefs
	 *            Variables of the module
	 * @return {varName} : [0..{maximum encoded value}] init {encoded int value}; ...
	 * @throws VarNameNotFoundException
	 */
	private String buildModuleVarsDecl(Set<StateVarDefinition<IStateVarValue>> moduleVarDefs)
			throws VarNameNotFoundException {
		StringBuilder builder = new StringBuilder();
		for (StateVarDefinition<IStateVarValue> stateVarDef : moduleVarDefs) {
			StateVar<IStateVarValue> initStateVar = mXMDP.getInitialStateVar(stateVarDef.getName());
			builder.append(INDENT);
			String varName = stateVarDef.getName();
			Integer encodedValue = mEncodings.getEncodedIntValue(initStateVar);
			Integer maxEncodedValue = mEncodings.getMaximumEncodedIntValue(stateVarDef);
			builder.append(varName);
			builder.append(" : [0..");
			builder.append(maxEncodedValue);
			builder.append("] init ");
			builder.append(encodedValue);
			builder.append(";");
			builder.append("\n");
		}
		return builder.toString();
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
			Integer encodedValue = mEncodings.getEncodedIntValue(var);
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
		for (Entry<Set<StateVar<IStateVarValue>>, Double> entry : probEffect) {
			Set<StateVar<IStateVarValue>> effect = entry.getKey();
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
	private String buildUpdate(Set<StateVar<IStateVarValue>> effect) {
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
		Integer encodedValue = mEncodings.getEncodedIntValue(updatedStateVar);
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(varName);
		builder.append("'");
		builder.append("=");
		builder.append(encodedValue);
		builder.append(")");
		return builder.toString();
	}

	private String buildRewards() {
		StringBuilder builder = new StringBuilder();
		// TODO
		return builder.toString();
	}

}
