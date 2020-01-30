package language.mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.exceptions.ActionDefinitionNotFoundException;

/**
 * {@link TransitionFunction} represents a probabilistic transition function (i.e., a set of {@link FactoredPSO}s) of an
 * MDP.
 * 
 * Note: Some {@link FactoredPSO}s in the {@link TransitionFunction} may be composite factored PSOs. However, the
 * composite factored PSOs can only be accessed directly via getActionPSO(). The iterator of {@link TransitionFunction}
 * only iterates over non-composite factored PSOs.
 * 
 * @author rsukkerd
 *
 */
public class TransitionFunction implements Iterable<FactoredPSO<IAction>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<FactoredPSO<? extends IAction>> mAllTransitions = new HashSet<>();

	// For iterator
	private Set<FactoredPSO<? extends IAction>> mNonCompositeTransitions = new HashSet<>();

	// For fast look-up
	private Map<ActionDefinition<? extends IAction>, FactoredPSO<? extends IAction>> mLookupTable = new HashMap<>();

	public TransitionFunction() {
		// mTransitions and mLookupTable initially empty
	}

	public void add(FactoredPSO<? extends IAction> actionPSO) {
		mAllTransitions.add(actionPSO);
		if (!actionPSO.getActionDefinition().isComposite()) {
			// Iterator will iterate over non-composite action PSOs only
			mNonCompositeTransitions.add(actionPSO);
		}
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

	/**
	 * This iterator only iterates over non-composite {@link FactoredPSO}s.
	 */
	@Override
	public Iterator<FactoredPSO<IAction>> iterator() {
		return new Iterator<FactoredPSO<IAction>>() {

			private Iterator<FactoredPSO<? extends IAction>> iter = mNonCompositeTransitions.iterator();

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
		return transFunc.mAllTransitions.equals(mAllTransitions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAllTransitions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
