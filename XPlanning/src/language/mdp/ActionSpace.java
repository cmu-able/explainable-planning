package language.mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;

/**
 * {@link ActionSpace} represents an action space (i.e., a set of {@link ActionDefinition}s) of an MDP.
 * 
 * Note: {@link ActionSpace} must NOT contain any composite action definition.
 * 
 * @author rsukkerd
 *
 */
public class ActionSpace implements Iterable<ActionDefinition<IAction>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<ActionDefinition<? extends IAction>> mActionDefs = new HashSet<>();
	private Map<IAction, ActionDefinition<? extends IAction>> mActionDefsLookup = new HashMap<>();
	private Map<String, IAction> mActionsLookup = new HashMap<>();

	public ActionSpace() {
		// mActionDefs initially empty
	}

	public void addActionDefinition(ActionDefinition<? extends IAction> actionDef) {
		mActionDefs.add(actionDef);

		for (IAction action : actionDef.getActions()) {
			mActionDefsLookup.put(action, actionDef);
			mActionsLookup.put(action.getName(), action);
		}
	}

	public boolean contains(ActionDefinition<? extends IAction> actionDef) {
		return mActionDefs.contains(actionDef);
	}

	public boolean contains(IAction action) {
		return mActionDefsLookup.containsKey(action);
	}

	public <E extends IAction> ActionDefinition<E> getActionDefinition(String actionName) {
		// Casting: ensure type-safety in the caller methods
		return (ActionDefinition<E>) mActionDefs.stream().filter(actionDef -> actionDef.getName().equals(actionName))
				.findFirst().orElse(null);
	}

	public <E extends IAction> ActionDefinition<E> getActionDefinition(E action) {
		// Casting: We ensure type-safety in addActionDefinition()
		return (ActionDefinition<E>) mActionDefsLookup.get(action);
	}

	public IAction getAction(String actionName) {
		return mActionsLookup.get(actionName);
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

			@Override
			public void remove() {
				iter.remove();
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
