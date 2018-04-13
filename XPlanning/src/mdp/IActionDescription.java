package mdp;

import java.util.Map;
import java.util.Map.Entry;

import exceptions.ActionNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;
import factors.IAction;

/**
 * {@link IActionDescription} is an interface to an action description for a specific {@link EffectClass} of a specific
 * action type. For each action of that type, an action description maps a set of mutually exclusive discriminants to
 * the corresponding probabilistic effects.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public interface IActionDescription<E extends IAction>
		extends Iterable<Entry<E, Map<Discriminant, ProbabilisticEffect>>> {

	public double getProbability(Effect effect, Discriminant discriminant, E action)
			throws ActionNotFoundException, DiscriminantNotFoundException, EffectNotFoundException;

	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, E action)
			throws ActionNotFoundException, DiscriminantNotFoundException;

	public DiscriminantClass getDiscriminantClass();

	public EffectClass getEffectClass();
}
