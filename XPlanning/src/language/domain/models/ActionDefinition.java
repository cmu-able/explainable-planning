package language.domain.models;

import java.util.HashSet;
import java.util.Set;

import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;

/**
 * {@link ActionDefinition} defines a set of actions of a particular type.
 * 
 * {@link ActionDefinition} can be a composite action definition. That is, it contains actions of multiple types that
 * are of the same super type. Composite action definition allows {@link IQFunction}, {@link IEvent}, and
 * {@link ITransitionStructure} to be defined over a generic action type.
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
	private boolean mIsComposite;

	public ActionDefinition(String name, E... actions) {
		mName = name;
		mActions = new HashSet<>();
		for (E action : actions) {
			mActions.add(action);
		}

		mIsComposite = checkComposite(mActions);
	}

	public ActionDefinition(String name, Set<? extends E> actions) {
		mName = name;
		mActions = new HashSet<>(actions);

		mIsComposite = checkComposite(mActions);
	}

	private boolean checkComposite(Set<E> actions) {
		return actions.stream().map(E::getClass).distinct().count() > 1;
	}

	public String getName() {
		return mName;
	}

	public Set<E> getActions() {
		return mActions;
	}

	public boolean isComposite() {
		return false;
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
		return actionDef.mName.equals(mName) && actionDef.mActions.equals(mActions)
				&& actionDef.mIsComposite == mIsComposite;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mActions.hashCode();
			result = 31 * result + Boolean.hashCode(mIsComposite);
			hashCode = result;
		}
		return result;
	}

	@Override
	public String toString() {
		return mName;
	}
}
