package mdp;

import java.util.Set;

import exceptions.ActionNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.IncompatibleVarException;
import factors.ActionDefinition;
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
public interface IActionDescription<E extends IAction> {

	public Set<ProbabilisticTransition<E>> getProbabilisticTransitions(E action)
			throws ActionNotFoundException, IncompatibleVarException;

	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, E action)
			throws ActionNotFoundException, DiscriminantNotFoundException;

	public ActionDefinition<E> getActionDefinition();

	public DiscriminantClass getDiscriminantClass();

	public EffectClass getEffectClass();
}
