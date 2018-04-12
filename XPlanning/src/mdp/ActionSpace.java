package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.ActionDefinition;
import factors.IAction;

/**
 * {@link ActionSpace} represents an action space (i.e., a set of {@link ActionDefinition}s) of an MDP.
 * 
 * @author rsukkerd
 *
 */
public class ActionSpace implements Iterable<ActionDefinition<IAction>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<ActionDefinition<? extends IAction>> mActionDefs;

	public ActionSpace() {
		mActionDefs = new HashSet<>();
	}

	public void addActionDefinition(ActionDefinition<? extends IAction> actionDef) {
		mActionDefs.add(actionDef);
	}

	@Override
	public Iterator<ActionDefinition<IAction>> iterator() {
		return new Iterator<ActionDefinition<IAction>>() {

			private Iterator<ActionDefinition<? extends IAction>> iter = mActionDefs.iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public ActionDefinition<IAction> next() {
				return (ActionDefinition<IAction>) iter.next();
			}
		};
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ActionSpace)) {
			return false;
		}
		ActionSpace actionSpace = (ActionSpace) obj;
		return actionSpace.mActionDefs.equals(mActionDefs);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDefs.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
