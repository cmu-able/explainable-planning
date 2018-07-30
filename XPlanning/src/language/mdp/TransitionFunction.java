package language.mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import language.exceptions.ActionDefinitionNotFoundException;
import language.qfactors.ActionDefinition;
import language.qfactors.IAction;

/**
 * {@link TransitionFunction} represents a probabilistic transition function (i.e., a set of {@link FactoredPSO}s) of an
 * MDP.
 * 
 * @author rsukkerd
 *
 */
public class TransitionFunction implements Iterable<FactoredPSO<IAction>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<FactoredPSO<? extends IAction>> mTransitions = new HashSet<>();
	// For fast look-up
	private Map<ActionDefinition<? extends IAction>, FactoredPSO<? extends IAction>> mLookupTable = new HashMap<>();

	public TransitionFunction() {
		// mTransitions and mLookupTable initially empty
	}

	public void add(FactoredPSO<? extends IAction> actionPSO) {
		mTransitions.add(actionPSO);
		mLookupTable.put(actionPSO.getActionDefinition(), actionPSO);
	}

	public <E extends IAction> FactoredPSO<E> getActionPSO(ActionDefinition<E> actionDefinition)
			throws ActionDefinitionNotFoundException {
		if (!mLookupTable.containsKey(actionDefinition)) {
			throw new ActionDefinitionNotFoundException(actionDefinition);
		}
		// Casting: We ensure type-safety in add()
		return (FactoredPSO<E>) mLookupTable.get(actionDefinition);
	}

	@Override
	public Iterator<FactoredPSO<IAction>> iterator() {
		return new Iterator<FactoredPSO<IAction>>() {

			private Iterator<FactoredPSO<? extends IAction>> iter = mTransitions.iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public FactoredPSO<IAction> next() {
				return (FactoredPSO<IAction>) iter.next();
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
		if (!(obj instanceof TransitionFunction)) {
			return false;
		}
		TransitionFunction transFunc = (TransitionFunction) obj;
		return transFunc.mTransitions.equals(mTransitions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTransitions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
