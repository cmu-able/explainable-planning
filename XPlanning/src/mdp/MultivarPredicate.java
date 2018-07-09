package mdp;

import java.util.HashSet;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVarDefinition;

public class MultivarPredicate implements IPreconditionPredicate {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVarDefinition<? extends IStateVarValue>> mStateVarDefs = new HashSet<>();
	private Set<IStatePredicate> mAllowableValueTuples = new HashSet<>();

	public MultivarPredicate(StateVarDefinition<? extends IStateVarValue>... stateVarDefs) {
		for (StateVarDefinition<? extends IStateVarValue> stateVarDef : stateVarDefs) {
			mStateVarDefs.add(stateVarDef);
		}
	}

	public void addAllowableValueTuple(IStatePredicate statePredicate) {
		mAllowableValueTuples.add(statePredicate);
	}

	public Set<IStatePredicate> getAllowableValueTuples() {
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
		return predicate.mStateVarDefs.equals(mStateVarDefs)
				&& predicate.mAllowableValueTuples.equals(mAllowableValueTuples);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarDefs.hashCode();
			result = 31 * result + mAllowableValueTuples.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
