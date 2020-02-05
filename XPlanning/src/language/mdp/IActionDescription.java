package language.mdp;

import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.exceptions.XMDPException;

/**
 * {@link IActionDescription} is an interface to an action description for a specific {@link EffectClass} of a specific
 * action type. For each action of that type, an action description maps a set of mutually exclusive discriminants of
 * the {@link DiscriminantClass} to the corresponding probabilistic effects.
 * 
 * Note: {@link IActionDescription} can be a composite action description. That is, it is for a generic action type,
 * whose action subtypes have the same effect class and discriminant class.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public interface IActionDescription<E extends IAction> {

	public Set<ProbabilisticTransition<E>> getProbabilisticTransitions(E action) throws XMDPException;

	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, E action) throws XMDPException;

	public ActionDefinition<E> getActionDefinition();

	public DiscriminantClass getDiscriminantClass();

	public EffectClass getEffectClass();
}
