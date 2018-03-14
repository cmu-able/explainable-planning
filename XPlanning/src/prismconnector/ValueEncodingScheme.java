package prismconnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;

/**
 * {@link ValueEncodingScheme} is an encoding scheme for representing the values of each state variable as PRISM's
 * supported types.
 * 
 * @author rsukkerd
 *
 */
public class ValueEncodingScheme {

	private Map<String, Map<IStateVarValue, Integer>> mEncodings;

	public ValueEncodingScheme(Set<StateVarDefinition<IStateVarValue>> stateVarDefs) {
		mEncodings = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateVarDefs) {
			String varName = stateVarDef.getName();
			Set<IStateVarValue> possibleValues = stateVarDef.getPossibleValues();
			Map<IStateVarValue, Integer> encoding = buildIntEncoding(possibleValues);
			mEncodings.put(varName, encoding);
		}
	}

	private Map<IStateVarValue, Integer> buildIntEncoding(Set<IStateVarValue> possibleValues) {
		Map<IStateVarValue, Integer> encoding = new HashMap<>();
		int e = 0;
		for (IStateVarValue value : possibleValues) {
			encoding.put(value, e);
			e++;
		}
		return encoding;
	}

	public Integer getEncodedIntValue(StateVar<IStateVarValue> stateVar) {
		return mEncodings.get(stateVar.getName()).get(stateVar.getValue());
	}

	public Integer getMaximumEncodedIntValue(StateVarDefinition<IStateVarValue> stateVarDef) {
		Map<IStateVarValue, Integer> encoding = mEncodings.get(stateVarDef.getName());
		return encoding.size() - 1;
	}
}
