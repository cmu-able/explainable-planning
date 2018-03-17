package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * {@link ActionDescription} is a generic action description of a specific {@link EffectClass}. An action description
 * maps a set of mutually exclusive discriminants to the corresponding probabilistic effects.
 * 
 * @author rsukkerd
 *
 */
public class ActionDescription implements IActionDescription {

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
	public EffectClass getEffectClass() {
		return mEffectClass;
	}
}
