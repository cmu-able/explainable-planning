package mdp;

import java.util.Iterator;
import java.util.Set;

/**
 * {@link Policy} contains a set of commands {@link Command}.
 * 
 * @author rsukkerd
 *
 */
public class Policy implements Iterable<Command>, Iterator<Command> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<Command> mCommands;

	public Policy(Set<Command> commands) {
		mCommands = commands;
	}

	@Override
	public boolean hasNext() {
		return mCommands.iterator().hasNext();
	}

	@Override
	public Command next() {
		return mCommands.iterator().next();
	}

	@Override
	public Iterator<Command> iterator() {
		return mCommands.iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Policy)) {
			return false;
		}
		Policy policy = (Policy) obj;
		return policy.mCommands.equals(mCommands);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mCommands.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
