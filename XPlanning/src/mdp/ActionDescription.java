package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import exceptions.ActionNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import factors.IAction;

/**
 * 
 * {@link ActionDescription} is a generic action description of a specific {@link EffectClass}. An action description
 * maps a set of mutually exclusive discriminants to the corresponding probabilistic effects.
 * 
 * @author rsukkerd
 *
 */
public class ActionDescription<E extends IAction> implements IActionDescription<E> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<E, Map<Discriminant, ProbabilisticEffect>> mActionDescriptions;
	private DiscriminantClass mDiscriminantClass;
	private EffectClass mEffectClass;

	public ActionDescription(DiscriminantClass discriminantClass, EffectClass effectClass) {
		mActionDescriptions = new HashMap<>();
		mDiscriminantClass = discriminantClass;
		mEffectClass = effectClass;
	}

	public void put(ProbabilisticEffect probEffect, Discriminant discriminant, E action)
			throws IncompatibleDiscriminantClassException, IncompatibleEffectClassException {
		if (!sanityCheck(discriminant)) {
			throw new IncompatibleDiscriminantClassException(discriminant.getDiscriminantClass());
		}
		if (!sanityCheck(probEffect)) {
			throw new IncompatibleEffectClassException(probEffect.getEffectClass());
		}
		if (!mActionDescriptions.containsKey(action)) {
			Map<Discriminant, ProbabilisticEffect> actionDesc = new HashMap<>();
			actionDesc.put(discriminant, probEffect);
			mActionDescriptions.put(action, actionDesc);
		} else {
			mActionDescriptions.get(action).put(discriminant, probEffect);
		}
	}

	private boolean sanityCheck(Discriminant discriminant) {
		return discriminant.getDiscriminantClass().equals(mDiscriminantClass);
	}

	private boolean sanityCheck(ProbabilisticEffect probEffect) {
		return probEffect.getEffectClass().equals(mEffectClass);
	}

	@Override
	public double getProbability(Effect effect, Discriminant discriminant, E action)
			throws ActionNotFoundException, DiscriminantNotFoundException, EffectNotFoundException {
		if (!mActionDescriptions.containsKey(action)) {
			throw new ActionNotFoundException(action);
		}
		if (!mActionDescriptions.get(action).containsKey(discriminant)) {
			throw new DiscriminantNotFoundException(discriminant);
		}
		return mActionDescriptions.get(action).get(discriminant).getProbability(effect);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, E action)
			throws ActionNotFoundException, DiscriminantNotFoundException {
		if (!mActionDescriptions.containsKey(action)) {
			throw new ActionNotFoundException(action);
		}
		if (!mActionDescriptions.get(action).containsKey(discriminant)) {
			throw new DiscriminantNotFoundException(discriminant);
		}
		return mActionDescriptions.get(action).get(discriminant);
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
	public Iterator<Entry<E, Map<Discriminant, ProbabilisticEffect>>> iterator() {
		return mActionDescriptions.entrySet().iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ActionDescription<?>)) {
			return false;
		}
		ActionDescription<?> actionDesc = (ActionDescription<?>) obj;
		return actionDesc.mActionDescriptions.equals(mActionDescriptions)
				&& actionDesc.mDiscriminantClass.equals(mDiscriminantClass)
				&& actionDesc.mEffectClass.equals(mEffectClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDescriptions.hashCode();
			result = 31 * result + mDiscriminantClass.hashCode();
			result = 31 * result + mEffectClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
