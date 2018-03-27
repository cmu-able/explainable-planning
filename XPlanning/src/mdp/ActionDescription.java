package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;

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
	private EffectClass mEffectClass;

	public ActionDescription(EffectClass effectClass) {
		mActionDescription = new HashMap<>();
		mEffectClass = effectClass;
	}

	public void put(Discriminant discriminant, ProbabilisticEffect probEffect) {
		mActionDescription.put(discriminant, probEffect);
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
		return actionDesc.mActionDescription.equals(mActionDescription) && actionDesc.mEffectClass.equals(mEffectClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDescription.hashCode();
			result = 31 * result + mEffectClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
