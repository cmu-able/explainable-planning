package mdp;

import java.util.Set;

import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;

/**
 * {@link Command} is a pair of a state description (i.e., a set of state variables) and an action.
 * 
 * @author rsukkerd
 *
 */
public class Command {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVar<IStateVarValue>> mGuard;
	private IAction mAction;

	public Command(Set<StateVar<IStateVarValue>> guard, IAction action) {
		mGuard = guard;
		mAction = action;
	}

	public Set<StateVar<IStateVarValue>> getGuard() {
		return mGuard;
	}

	public IAction getAction() {
		return mAction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Command)) {
			return false;
		}
		Command cmd = (Command) obj;
		return cmd.mGuard.equals(mGuard) && cmd.mAction.equals(mAction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mGuard.hashCode();
			result = 31 * result + mAction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
