package prismconnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVarDefinition;
import mdp.StateSpace;

/**
 * {@link ValueEncodingScheme} is an encoding scheme for representing the values of each state variable as PRISM's
 * supported types. In the case of 3-parameter reward function: R(s,a,s'), this is also an encoding scheme for actions.
 * 
 * @author rsukkerd
 *
 */
public class ValueEncodingScheme {

	private Map<StateVarDefinition<IStateVarValue>, Map<IStateVarValue, Integer>> mStateVarEncodings;
	private Map<ActionDefinition<IAction>, Map<IAction, Integer>> mActionEncodings;

	public ValueEncodingScheme(StateSpace stateSpace, Set<ActionDefinition<IAction>> actionDefs) {
		mStateVarEncodings = new HashMap<>();
		mActionEncodings = new HashMap<>();

		for (StateVarDefinition<IStateVarValue> stateVarDef : stateSpace) {
			Map<IStateVarValue, Integer> encoding = buildIntEncoding(stateVarDef.getPossibleValues());
			mStateVarEncodings.put(stateVarDef, encoding);
		}
		for (ActionDefinition<IAction> actionDef : actionDefs) {
			Map<IAction, Integer> encoding = buildIntEncoding(actionDef.getActions());
			mActionEncodings.put(actionDef, encoding);
		}
	}

	private <E> Map<E, Integer> buildIntEncoding(Set<E> possibleValues) {
		Map<E, Integer> encoding = new HashMap<>();
		int e = 0;
		for (E value : possibleValues) {
			encoding.put(value, e);
			e++;
		}
		return encoding;
	}

	public <E extends IStateVarValue> Integer getEncodedIntValue(StateVarDefinition<E> stateVarDef, E value) {
		return mStateVarEncodings.get(stateVarDef).get(value);
	}

	public <E extends IAction> Integer getEncodedIntValue(ActionDefinition<E> actionDef, E action) {
		return mActionEncodings.get(actionDef).get(action);
	}

	public Integer getMaximumEncodedIntValue(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		Map<IStateVarValue, Integer> encoding = mStateVarEncodings.get(stateVarDef);
		return encoding.size() - 1;
	}

	public Integer getMaximumEncodedIntValue(ActionDefinition<? extends IAction> actionDef) {
		Map<IAction, Integer> encoding = mActionEncodings.get(actionDef);
		return encoding.size() - 1;
	}
}
