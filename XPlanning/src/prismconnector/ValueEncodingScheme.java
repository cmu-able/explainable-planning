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
			Map<IStateVarValue, Integer> encoding = buildIntEncoding(stateVarDef.getPossibleValues());
			mEncodings.put(stateVarDef.getName(), encoding);
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

	public <E extends IStateVarValue> Integer getEncodedIntValue(StateVarDefinition<E> stateVarDef, E value) {
		return mEncodings.get(stateVarDef.getName()).get(value);
	}

	public Integer getEncodedIntValue(StateVar<? extends IStateVarValue> stateVar) {
		return mEncodings.get(stateVar.getName()).get(stateVar.getValue());
	}

	public Integer getMaximumEncodedIntValue(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		Map<IStateVarValue, Integer> encoding = mEncodings.get(stateVarDef.getName());
		return encoding.size() - 1;
	}
}
