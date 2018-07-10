package mdp;

import java.util.HashSet;
import java.util.Set;

import exceptions.IncompatibleVarsException;
import factors.IStateVarValue;
import factors.StateVar;

/**
 * {@link MultivarPredicate} defines a precondition over a set of state variables. It contains a set of tuples of
 * allowable values of the variables.
 * 
 * @author rsukkerd
 *
 */
public class MultivarPredicate implements IPreconditionPredicate {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private MultivarClass mMultivarClass;
	private Set<StatePredicate> mAllowableValueTuples = new HashSet<>();

	public MultivarPredicate(MultivarClass multivarClass) {
		mMultivarClass = multivarClass;
	}

	public void addAllowableValueTuple(StatePredicate statePredicate) throws IncompatibleVarsException {
		if (!sanityCheck(statePredicate)) {
			throw new IncompatibleVarsException(statePredicate);
		}
		mAllowableValueTuples.add(statePredicate);
	}

	private boolean sanityCheck(StatePredicate statePredicate) {
		int predicateSize = 0;
		for (StateVar<IStateVarValue> stateVar : statePredicate) {
			predicateSize++;
			if (!mMultivarClass.contains(stateVar.getDefinition())) {
				return false;
			}
		}
		return predicateSize == mMultivarClass.size();
	}

	public MultivarClass getMultivarClass() {
		return mMultivarClass;
	}

	public Set<StatePredicate> getAllowableValueTuples() {
		return mAllowableValueTuples;
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
		return predicate.mMultivarClass.equals(mMultivarClass)
				&& predicate.mAllowableValueTuples.equals(mAllowableValueTuples);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMultivarClass.hashCode();
			result = 31 * result + mAllowableValueTuples.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
