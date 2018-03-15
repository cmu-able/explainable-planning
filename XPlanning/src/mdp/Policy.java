package mdp;

import java.util.Iterator;
import java.util.Set;

/**
 * {@link Policy} contains a set of commands {@link Decision}.
 * 
 * @author rsukkerd
 *
 */
public class Policy implements Iterable<Decision>, Iterator<Decision> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<Decision> mCommands;

	public Policy(Set<Decision> commands) {
		mCommands = commands;
	}

	@Override
	public boolean hasNext() {
		return mCommands.iterator().hasNext();
	}

	@Override
	public Decision next() {
		return mCommands.iterator().next();
	}

	@Override
	public Iterator<Decision> iterator() {
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
