package prismconnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.VarNameNotFoundException;
import factors.IAction;
import factors.IStateVarValue;
import factors.ProbabilisticEffect;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.EffectClass;
import mdp.IFactoredPSO;
import mdp.Policy;
import mdp.XMDP;
import metrics.IQFunction;

public class PRISMTranslator {

	private static final String INDENT = "  ";

	private XMDP mXMDP;

	private ValueEncodingScheme mEncodings;

	public PRISMTranslator(XMDP xmdp) {
		mXMDP = xmdp;
	}

	public String getMDPTranslation() {
		return null;
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

	private Map<IStateVarValue, Integer> getStateVarValuesIntEncoding(StateVarDefinition<IStateVarValue> stateVarDef) {
		Map<IStateVarValue, Integer> encoding = new HashMap<>();
		int e = 0;
		for (IStateVarValue value : stateVarDef.getPossibleValues()) {
			encoding.put(value, e);
			e++;
		}
		return encoding;
	}

	private String getAllConstsDecl(Set<StateVarDefinition<IStateVarValue>> stateVarDefs) {
		StringBuilder builder = new StringBuilder();
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateVarDefs) {
			String decl = getConstsDecl(stateVarDef);
			builder.append(decl);
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param stateVarDef
	 * @return const int {varName}_{value} = {encoded int value};
	 */
	private String getConstsDecl(StateVarDefinition<IStateVarValue> stateVarDef) {
		StringBuilder builder = new StringBuilder();
		String varName = stateVarDef.getName();
		builder.append("// Possible values of ");
		builder.append(varName);
		builder.append("\n");
		Map<IStateVarValue, Integer> encoding = getStateVarValuesIntEncoding(stateVarDef);
		for (Entry<IStateVarValue, Integer> e : encoding.entrySet()) {
			IStateVarValue value = e.getKey();
			Integer encodedValue = e.getValue();
			builder.append("const int ");
			builder.append(varName);
			builder.append("_");
			builder.append(value.toString());
			builder.append(" = ");
			builder.append(encodedValue.toString());
			builder.append(";");
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param name
	 * @param stateVarDefs
	 * @param actionPSO
	 * @return module {name} ... endmodule
	 * @throws VarNameNotFoundException
	 */
	private String buildModule(String name, Set<StateVarDefinition<IStateVarValue>> stateVarDefs,
			IFactoredPSO actionPSO) throws VarNameNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("module ");
		builder.append(name);
		builder.append("\n");
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateVarDefs) {
			StateVar<IStateVarValue> initStateVar = mXMDP.getInitialStateVar(stateVarDef.getName());
			String varDecl = buildModuleVarDecl(stateVarDef, initStateVar);
			builder.append(INDENT);
			builder.append(varDecl);
			builder.append("\n");
		}
		builder.append("\n");
		builder.append("endmodule");
		return builder.toString();
	}

	/**
	 * 
	 * @param initStateVar
	 * @return {varName} : [0..{maximum encoded value}] init {encoded int value};
	 */
	private String buildModuleVarDecl(StateVarDefinition<IStateVarValue> stateVarDef,
			StateVar<IStateVarValue> initStateVar) {
		StringBuilder builder = new StringBuilder();
		String varName = initStateVar.getName();
		Integer encodedValue = mEncodings.getEncodedIntValue(initStateVar);
		Integer maxEncodedValue = mEncodings.getMaximumEncodedIntValue(stateVarDef);
		builder.append(varName);
		builder.append(" : [0..");
		builder.append(maxEncodedValue);
		builder.append("] init ");
		builder.append(encodedValue);
		builder.append(";");
		return builder.toString();
	}

	private String buildModuleCommands(IFactoredPSO actionPSO, EffectClass effectClass) {
		StringBuilder builder = new StringBuilder();
		// TODO
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
	 * @param stateVar
	 * @return ({varName}'={encoded int value})
	 */
	private String buildVariableUpdate(StateVar<IStateVarValue> stateVar) {
		String varName = stateVar.getName();
		Integer encodedValue = mEncodings.getEncodedIntValue(stateVar);
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		builder.append(varName);
		builder.append("'");
		builder.append("=");
		builder.append(encodedValue);
		builder.append(")");
		return builder.toString();
	}

}
