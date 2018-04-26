package policy;

import factors.IAction;

/**
 * {@link Decision} is a pair of a state {@link Predicate} and an action.
 * 
 * @author rsukkerd
 *
 */
public class Decision {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Predicate mPredicate;
	private IAction mAction;

	public Decision(Predicate state, IAction action) {
		mPredicate = state;
		mAction = action;
	}

	public Predicate getPredicate() {
		return mPredicate;
	}

	public IAction getAction() {
		return mAction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Decision)) {
			return false;
		}
		Decision decision = (Decision) obj;
		return decision.mPredicate.equals(mPredicate) && decision.mAction.equals(mAction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPredicate.hashCode();
			result = 31 * result + mAction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
