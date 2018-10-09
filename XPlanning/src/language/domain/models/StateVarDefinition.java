package language.domain.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link StateVarDefinition} defines a set of possible values of a specific state variable.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class StateVarDefinition<E extends IStateVarValue> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mName;
	private Set<E> mPossibleValues;

	// For retrieving a state variable of a particular value
	private Map<E, StateVar<E>> mStateVars = new HashMap<>();

	public StateVarDefinition(String name, E... possibleValues) {
		mName = name;
		mPossibleValues = new HashSet<>();
		for (E value : possibleValues) {
			mPossibleValues.add(value);
		}
		buildStateVariables();
	}

	public StateVarDefinition(String name, Set<? extends E> possibleValues) {
		mName = name;
		mPossibleValues = new HashSet<>(possibleValues);
		buildStateVariables();
	}

	private void buildStateVariables() {
		for (E value : mPossibleValues) {
			StateVar<E> stateVar = new StateVar<>(this, value);
			mStateVars.put(value, stateVar);
		}
	}

	public String getName() {
		return mName;
	}

	public Set<E> getPossibleValues() {
		return mPossibleValues;
	}

	public StateVar<E> getStateVar(E value) {
		return mStateVars.get(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof StateVarDefinition<?>)) {
			return false;
		}
		StateVarDefinition<?> varDef = (StateVarDefinition<?>) obj;
		return varDef.mName.equals(mName) && varDef.mPossibleValues.equals(mPossibleValues);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mPossibleValues.hashCode();
			hashCode = result;
		}
		return result;
	}

	@Override
	public String toString() {
		return getName();
	}
}
