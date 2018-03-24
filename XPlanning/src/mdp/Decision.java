package mdp;

import factors.IAction;

/**
 * {@link Decision} is a pair of a state description (i.e., a set of state variables) and an action.
 * 
 * @author rsukkerd
 *
 */
public class Decision {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Discriminant mDiscriminant;
	private IAction mAction;

	public Decision(Discriminant guard, IAction action) {
		mDiscriminant = guard;
		mAction = action;
	}

	public Discriminant getDiscriminant() {
		return mDiscriminant;
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
		return decision.mDiscriminant.equals(mDiscriminant) && decision.mAction.equals(mAction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDiscriminant.hashCode();
			result = 31 * result + mAction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
