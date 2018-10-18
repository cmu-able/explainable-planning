package language.mdp;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link MultivarPredicate} is a disjunction of allowable value tuples of a particular class of state variables. It
 * contains a set of allowable value tuples of the class of variables.
 * 
 * @author rsukkerd
 *
 */
public class MultivarPredicate implements IPreconditionPredicate {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarClass mStateVarClass;
	private Set<StateVarTuple> mAllowableTuples = new HashSet<>();

	public MultivarPredicate(StateVarClass stateVarClass) {
		mStateVarClass = stateVarClass;
	}

	public void addAllowableTuple(StateVarTuple tuple) {
		mAllowableTuples.add(tuple);
	}

	public StateVarClass getStateVarClass() {
		return mStateVarClass;
	}

	public Set<StateVarTuple> getAllowableTuples() {
		return mAllowableTuples;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof MultivarPredicate)) {
			return false;
		}
		MultivarPredicate predicate = (MultivarPredicate) obj;
		return predicate.mStateVarClass.equals(mStateVarClass) && predicate.mAllowableTuples.equals(mAllowableTuples);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarClass.hashCode();
			result = 31 * result + mAllowableTuples.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
