package prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.ActionNotFoundException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVarDefinition;
import mdp.ActionSpace;
import mdp.StateSpace;

/**
 * {@link ValueEncodingScheme} is an encoding scheme for representing the values of each state variable as PRISM's
 * supported types. In the case of 3-parameter reward function: R(s,a,s'), this is also an encoding scheme for actions.
 * 
 * @author rsukkerd
 *
 */
public class ValueEncodingScheme {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<StateVarDefinition<IStateVarValue>, Map<IStateVarValue, Integer>> mStateVarEncodings = new HashMap<>();
	private Map<IAction, Integer> mActionEncoding = new HashMap<>();
	private StateSpace mStateSpace;
	private ActionSpace mActionSpace;

	public ValueEncodingScheme(StateSpace stateSpace) {
		mStateSpace = stateSpace;
		encodeStates(stateSpace);
	}

	public ValueEncodingScheme(StateSpace stateSpace, ActionSpace actionSpace) {
		mStateSpace = stateSpace;
		mActionSpace = actionSpace;
		encodeStates(stateSpace);
		encodeActions(actionSpace);
	}

	private void encodeStates(StateSpace stateSpace) {
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateSpace) {
			Map<IStateVarValue, Integer> encoding = buildIntEncoding(stateVarDef.getPossibleValues());
			mStateVarEncodings.put(stateVarDef, encoding);
		}
	}

	private void encodeActions(ActionSpace actionSpace) {
		Set<IAction> allActions = new HashSet<>();
		for (ActionDefinition<IAction> actionDef : actionSpace) {
			allActions.addAll(actionDef.getActions());
		}
		mActionEncoding.putAll(buildIntEncoding(allActions));
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

	public StateSpace getStateSpace() {
		return mStateSpace;
	}

	public ActionSpace getActionSpace() {
		return mActionSpace;
	}

	public <E extends IStateVarValue> Integer getEncodedIntValue(StateVarDefinition<E> stateVarDef, E value)
			throws VarNotFoundException {
		if (!mStateVarEncodings.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		return mStateVarEncodings.get(stateVarDef).get(value);
	}

	public Integer getEncodedIntValue(IAction action) throws ActionNotFoundException {
		if (!mActionEncoding.containsKey(action)) {
			throw new ActionNotFoundException(action);
		}
		return mActionEncoding.get(action);
	}

	public Integer getMaximumEncodedIntValue(StateVarDefinition<? extends IStateVarValue> stateVarDef)
			throws VarNotFoundException {
		if (!mStateVarEncodings.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		Map<IStateVarValue, Integer> encoding = mStateVarEncodings.get(stateVarDef);
		return encoding.size() - 1;
	}

	public Integer getMaximumEncodedIntAction() {
		return mActionEncoding.size() - 1;
	}

	public <E extends IStateVarValue> E decodeStateVarValue(Class<E> valueType, String stateVarName,
			Integer encodedIntValue) throws VarNotFoundException {
		for (Entry<StateVarDefinition<IStateVarValue>, Map<IStateVarValue, Integer>> entry : mStateVarEncodings
				.entrySet()) {
			StateVarDefinition<IStateVarValue> stateVarDef = entry.getKey();
			Map<IStateVarValue, Integer> encoding = entry.getValue();

			if (stateVarDef.getName().equals(stateVarName)) {
				for (Entry<IStateVarValue, Integer> e : encoding.entrySet()) {
					IStateVarValue value = e.getKey();
					Integer encodedValue = e.getValue();

					if (encodedValue.equals(encodedIntValue)) {
						return valueType.cast(value);
					}
				}
			}
		}
		throw new VarNotFoundException(stateVarName);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ValueEncodingScheme)) {
			return false;
		}
		ValueEncodingScheme scheme = (ValueEncodingScheme) obj;
		return scheme.mStateVarEncodings.equals(mStateVarEncodings) && scheme.mActionEncoding.equals(mActionEncoding);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarEncodings.hashCode();
			result = 31 * result + mActionEncoding.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
