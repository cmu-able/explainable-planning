package mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import exceptions.ActionNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import factors.ActionDefinition;
import factors.IAction;

/**
 * {@link TabularActionDescription} is a generic action description of a specific {@link EffectClass}. A "tabular"
 * action description explicitly maps a set of mutually exclusive discriminants to the corresponding probabilistic
 * effects.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class TabularActionDescription<E extends IAction> implements IActionDescription<E> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDefinition<E> mActionDefinition;
	private DiscriminantClass mDiscriminantClass;
	private EffectClass mEffectClass;

	private Map<E, Set<ProbabilisticTransition<E>>> mProbTransitions = new HashMap<>();
	private Map<E, Map<Discriminant, ProbabilisticEffect>> mLookupTable = new HashMap<>(); // For fast look-up

	public TabularActionDescription(ActionDefinition<E> actionDefinition, DiscriminantClass discriminantClass,
			EffectClass effectClass) {
		mActionDefinition = actionDefinition;
		mDiscriminantClass = discriminantClass;
		mEffectClass = effectClass;
	}

	public void put(ProbabilisticEffect probEffect, Discriminant discriminant, E action)
			throws IncompatibleActionException, IncompatibleDiscriminantClassException,
			IncompatibleEffectClassException {
		ProbabilisticTransition<E> probTrans = new ProbabilisticTransition<>(probEffect, discriminant, action);
		put(probTrans);
	}

	public void putAll(Set<ProbabilisticTransition<E>> probTransitions) throws IncompatibleActionException,
			IncompatibleDiscriminantClassException, IncompatibleEffectClassException {
		for (ProbabilisticTransition<E> probTrans : probTransitions) {
			put(probTrans);
		}
	}

	private void put(ProbabilisticTransition<E> probTrans) throws IncompatibleActionException,
			IncompatibleDiscriminantClassException, IncompatibleEffectClassException {
		E action = probTrans.getAction();
		Discriminant discriminant = probTrans.getDiscriminant();
		ProbabilisticEffect probEffect = probTrans.getProbabilisticEffect();

		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}
		if (!sanityCheck(discriminant)) {
			throw new IncompatibleDiscriminantClassException(discriminant.getDiscriminantClass());
		}
		if (!sanityCheck(probEffect)) {
			throw new IncompatibleEffectClassException(probEffect.getEffectClass());
		}
		if (!mProbTransitions.containsKey(action)) {
			Set<ProbabilisticTransition<E>> actionDesc = new HashSet<>();
			actionDesc.add(probTrans);
			mProbTransitions.put(action, actionDesc);

			// For fast look-up
			Map<Discriminant, ProbabilisticEffect> table = new HashMap<>();
			table.put(discriminant, probEffect);
			mLookupTable.put(action, table);
		} else {
			mProbTransitions.get(action).add(probTrans);

			// For fast look-up
			mLookupTable.get(action).put(discriminant, probEffect);
		}
	}

	private boolean sanityCheck(E action) {
		return mActionDefinition.getActions().contains(action);
	}

	private boolean sanityCheck(Discriminant discriminant) {
		return discriminant.getDiscriminantClass().equals(mDiscriminantClass);
	}

	private boolean sanityCheck(ProbabilisticEffect probEffect) {
		return probEffect.getEffectClass().equals(mEffectClass);
	}

	@Override
	public Set<ProbabilisticTransition<E>> getProbabilisticTransitions(E action)
			throws ActionNotFoundException, IncompatibleVarException {
		if (!mProbTransitions.containsKey(action)) {
			throw new ActionNotFoundException(action);
		}
		return mProbTransitions.get(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, E action)
			throws ActionNotFoundException, DiscriminantNotFoundException {
		if (!mLookupTable.containsKey(action)) {
			throw new ActionNotFoundException(action);
		}
		if (!mLookupTable.get(action).containsKey(discriminant)) {
			throw new DiscriminantNotFoundException(discriminant);
		}
		return mLookupTable.get(action).get(discriminant);
	}

	@Override
	public ActionDefinition<E> getActionDefinition() {
		return mActionDefinition;
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mDiscriminantClass;
	}

	@Override
	public EffectClass getEffectClass() {
		return mEffectClass;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TabularActionDescription<?>)) {
			return false;
		}
		TabularActionDescription<?> actionDesc = (TabularActionDescription<?>) obj;
		return actionDesc.mProbTransitions.equals(mProbTransitions)
				&& actionDesc.mActionDefinition.equals(mActionDefinition)
				&& actionDesc.mDiscriminantClass.equals(mDiscriminantClass)
				&& actionDesc.mEffectClass.equals(mEffectClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mProbTransitions.hashCode();
			result = 31 * result + mActionDefinition.hashCode();
			result = 31 * result + mDiscriminantClass.hashCode();
			result = 31 * result + mEffectClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
