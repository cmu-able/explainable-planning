package language.mdp;

import java.util.HashSet;
import java.util.Set;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;

/**
 * {@link UnivarPredicate} is a disjunction of allowable values of a particular state variable. It contains a set of
 * allowable values of the variable.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class UnivarPredicate<E extends IStateVarValue> implements IPreconditionPredicate {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<E> mStateVarDef;
	private Set<E> mAllowableValues = new HashSet<>();

	public UnivarPredicate(StateVarDefinition<E> stateVarDef) {
		mStateVarDef = stateVarDef;
	}

	public void addAllowableValue(E value) {
		mAllowableValues.add(value);
	}

	public StateVarDefinition<E> getStateVarDefinition() {
		return mStateVarDef;
	}

	public Set<E> getAllowableValues() {
		return mAllowableValues;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof UnivarPredicate<?>)) {
			return false;
		}
		UnivarPredicate<?> predicate = (UnivarPredicate<?>) obj;
		return predicate.mStateVarDef.equals(mStateVarDef) && predicate.mAllowableValues.equals(mAllowableValues);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarDef.hashCode();
			result = 31 * result + mAllowableValues.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
