package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;

/**
 * 
 * {@link ActionDescription} is a generic action description of a specific {@link EffectClass}. An action description
 * maps a set of mutually exclusive discriminants to the corresponding probabilistic effects.
 * 
 * @author rsukkerd
 *
 */
public class ActionDescription implements IActionDescription {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<Discriminant, ProbabilisticEffect> mActionDescription;
	private DiscriminantClass mDiscriminantClass;
	private EffectClass mEffectClass;

	public ActionDescription(DiscriminantClass discriminantClass, EffectClass effectClass) {
		mActionDescription = new HashMap<>();
		mDiscriminantClass = discriminantClass;
		mEffectClass = effectClass;
	}

	public void put(Discriminant discriminant, ProbabilisticEffect probEffect)
			throws IncompatibleDiscriminantClassException, IncompatibleEffectClassException {
		if (!sanityCheck(discriminant)) {
			throw new IncompatibleDiscriminantClassException(discriminant.getDiscriminantClass());
		}
		if (!sanityCheck(probEffect)) {
			throw new IncompatibleEffectClassException(probEffect.getEffectClass());
		}
		mActionDescription.put(discriminant, probEffect);
	}

	private boolean sanityCheck(Discriminant discriminant) {
		return discriminant.getDiscriminantClass().equals(mDiscriminantClass);
	}

	private boolean sanityCheck(ProbabilisticEffect probEffect) {
		return probEffect.getEffectClass().equals(mEffectClass);
	}

	@Override
	public Iterator<Entry<Discriminant, ProbabilisticEffect>> iterator() {
		return mActionDescription.entrySet().iterator();
	}

	@Override
	public double getProbability(Effect effect, Discriminant discriminant)
			throws DiscriminantNotFoundException, EffectNotFoundException {
		if (!mActionDescription.containsKey(discriminant)) {
			throw new DiscriminantNotFoundException(discriminant);
		}
		return mActionDescription.get(discriminant).getProbability(effect);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant) throws DiscriminantNotFoundException {
		if (!mActionDescription.containsKey(discriminant)) {
			throw new DiscriminantNotFoundException(discriminant);
		}
		return mActionDescription.get(discriminant);
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
		if (!(obj instanceof ActionDescription)) {
			return false;
		}
		ActionDescription actionDesc = (ActionDescription) obj;
		return actionDesc.mActionDescription.equals(mActionDescription)
				&& actionDesc.mDiscriminantClass.equals(mDiscriminantClass)
				&& actionDesc.mEffectClass.equals(mEffectClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDescription.hashCode();
			result = 31 * result + mDiscriminantClass.hashCode();
			result = 31 * result + mEffectClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
