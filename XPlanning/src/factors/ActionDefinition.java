package factors;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ActionDefinition} defines a set of actions of a particular type.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class ActionDefinition<E extends IAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mName;
	private Set<E> mActions;

	public ActionDefinition(String name, E... actions) {
		mName = name;
		mActions = new HashSet<>();
		for (E action : actions) {
			mActions.add(action);
		}
	}

	public ActionDefinition(String name, Set<? extends E> actions) {
		mName = name;
		mActions = new HashSet<>(actions);
	}

	public String getName() {
		return mName;
	}

	public Set<E> getActions() {
		return mActions;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ActionDefinition<?>)) {
			return false;
		}
		ActionDefinition<?> actionDef = (ActionDefinition<?>) obj;
		return actionDef.mName.equals(mName) && actionDef.mActions.equals(mActions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mActions.hashCode();
			hashCode = result;
		}
		return result;
	}

	@Override
	public String toString() {
		return mName;
	}
}
