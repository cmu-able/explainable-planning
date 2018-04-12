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

	private Set<ActionDefinition<IAction>> mActionDefs;

	public ActionSpace() {
		mActionDefs = new HashSet<>();
	}

	public void addActionDefinition(ActionDefinition<? extends IAction> actionDef) {
		ActionDefinition<IAction> genericActionDef = new ActionDefinition<>(actionDef.getName(),
				actionDef.getActions());
		mActionDefs.add(genericActionDef);
	}

	@Override
	public Iterator<ActionDefinition<IAction>> iterator() {
		return mActionDefs.iterator();
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
