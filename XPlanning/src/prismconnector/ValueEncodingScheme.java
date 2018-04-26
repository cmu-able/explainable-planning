package prismconnector;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import exceptions.ActionDefinitionNotFoundException;
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
	private Map<ActionDefinition<IAction>, Map<IAction, Integer>> mActionEncodings = new HashMap<>();

	public ValueEncodingScheme(StateSpace stateSpace) {
		encodeStates(stateSpace);
	}

	public ValueEncodingScheme(StateSpace stateSpace, ActionSpace actionSpace) {
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
		for (ActionDefinition<IAction> actionDef : actionSpace) {
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

	public <E extends IStateVarValue> Integer getEncodedIntValue(StateVarDefinition<E> stateVarDef, E value)
			throws VarNotFoundException {
		if (!mStateVarEncodings.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		return mStateVarEncodings.get(stateVarDef).get(value);
	}

	public <E extends IAction> Integer getEncodedIntValue(ActionDefinition<E> actionDef, E action)
			throws ActionDefinitionNotFoundException {
		if (!mActionEncodings.containsKey(actionDef)) {
			throw new ActionDefinitionNotFoundException(actionDef);
		}
		return mActionEncodings.get(actionDef).get(action);
	}

	public Integer getMaximumEncodedIntValue(StateVarDefinition<? extends IStateVarValue> stateVarDef)
			throws VarNotFoundException {
		if (!mStateVarEncodings.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		Map<IStateVarValue, Integer> encoding = mStateVarEncodings.get(stateVarDef);
		return encoding.size() - 1;
	}

	public Integer getMaximumEncodedIntValue(ActionDefinition<? extends IAction> actionDef)
			throws ActionDefinitionNotFoundException {
		if (!mActionEncodings.containsKey(actionDef)) {
			throw new ActionDefinitionNotFoundException(actionDef);
		}
		Map<IAction, Integer> encoding = mActionEncodings.get(actionDef);
		return encoding.size() - 1;
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
		return scheme.mStateVarEncodings.equals(mStateVarEncodings) && scheme.mActionEncodings.equals(mActionEncodings);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarEncodings.hashCode();
			result = 31 * result + mActionEncodings.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
