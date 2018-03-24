package mdp;

import java.util.Map.Entry;
import java.util.Set;

import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;

/**
 * {@link IActionDescription} is an interface to an action description for a specific {@link EffectClass}. An action
 * description maps a set of mutually exclusive discriminants to the corresponding probabilistic effects.
 * 
 * @author rsukkerd
 *
 */
public interface IActionDescription extends Iterable<Entry<Discriminant, ProbabilisticEffect>> {

	public double getProbability(Effect effect, Discriminant discriminant)
			throws DiscriminantNotFoundException, EffectNotFoundException;

	public Set<Effect> getPossibleEffects();

	public EffectClass getEffectClass();
}
