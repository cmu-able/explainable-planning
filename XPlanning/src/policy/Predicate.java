package policy;

import java.util.HashSet;
import java.util.Set;

import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.State;

/**
 * {@link Predicate} is a description of a state.
 * 
 * @author rsukkerd
 *
 */
public class Predicate {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVar<? extends IStateVarValue>> mStateVars = new HashSet<>();
	private State mState = new State(); // For fast look-up

	public Predicate() {
		// mStateVars is initially empty
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) {
		mStateVars.add(stateVar);
		mState.addStateVar(stateVar);
	}

	public <E extends IStateVarValue> E getStateVarValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException {
		return mState.getStateVarValue(valueType, stateVarDef);
	}

	public boolean isTrue(State state) {
		for (StateVar<? extends IStateVarValue> stateVar : mStateVars) {
			if (!state.contains(stateVar)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Predicate)) {
			return false;
		}
		Predicate predicate = (Predicate) obj;
		return predicate.mStateVars.equals(mStateVars);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVars.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
