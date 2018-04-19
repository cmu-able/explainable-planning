package mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVarDefinition;

/**
 * {@link Precondition} is a precondition of an action.
 * 
 * @author rsukkerd
 *
 */
public class Precondition {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<StateVarDefinition<? extends IStateVarValue>, Set<IStateVarValue>> mApplicableValues = new HashMap<>();

	public Precondition() {
		// mApplicableValues initially empty
	}

	public <E extends IStateVarValue> void add(StateVarDefinition<E> stateVarDef, E value) {
		if (!mApplicableValues.containsKey(stateVarDef)) {
			Set<IStateVarValue> applicableValues = new HashSet<>();
			applicableValues.add(value);
			mApplicableValues.put(stateVarDef, applicableValues);
		} else {
			mApplicableValues.get(stateVarDef).add(value);
		}
	}

	public <E extends IStateVarValue> Set<E> getApplicableValues(StateVarDefinition<E> stateVarDef) {
		if (mApplicableValues.containsKey(stateVarDef)) {
			return (Set<E>) mApplicableValues.get(stateVarDef);
		}
		return stateVarDef.getPossibleValues();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Precondition)) {
			return false;
		}
		Precondition precond = (Precondition) obj;
		return precond.mApplicableValues.equals(mApplicableValues);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mApplicableValues.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
