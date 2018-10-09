package language.policy;

import language.domain.models.IAction;
import language.mdp.StateVarTuple;

/**
 * {@link Decision} is a pair of a state and an action.
 * 
 * @author rsukkerd
 *
 */
public class Decision {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarTuple mState;
	private IAction mAction;

	public Decision(StateVarTuple state, IAction action) {
		mState = state;
		mAction = action;
	}

	public StateVarTuple getState() {
		return mState;
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
		return decision.mState.equals(mState) && decision.mAction.equals(mAction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mState.hashCode();
			result = 31 * result + mAction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
