package prismconnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Policy;
import mdp.XMDP;
import metrics.IQFunction;

public class PRISMTranslator {

	private XMDP mXMDP;

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
	 * @param stateVarInit
	 * @param encoding
	 * @return {varName} : [0..{maximum value}] init {encoded int value};
	 */
	private String getStateVarDecl(StateVar<IStateVarValue> stateVarInit, Map<IStateVarValue, Integer> encoding) {
		StringBuilder builder = new StringBuilder();
		String varName = stateVarInit.getName();
		IStateVarValue value = stateVarInit.getValue();
		int numPossibleValues = encoding.size();
		int encodedValue = encoding.get(value);
		builder.append(varName);
		builder.append(" : [0..");
		builder.append(Integer.toString(numPossibleValues - 1));
		builder.append("] init ");
		builder.append(Integer.toString(encodedValue));
		builder.append(";");
		return builder.toString();
	}

}
