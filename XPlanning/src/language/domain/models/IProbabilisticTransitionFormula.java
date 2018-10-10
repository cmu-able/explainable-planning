package language.domain.models;

import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.ProbabilisticEffect;

/**
 * {@link IProbabilisticTransitionFormula} is an interface for a probabilistic function that calculates probability
 * distribution of an effect class, given a discriminant and an action. The probabilistic function may access the
 * attributes of the state variables and action.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public interface IProbabilisticTransitionFormula<E extends IAction> {

	/**
	 * Calculate the probabilistic effect of a given action and a discriminant.
	 * 
	 * @param discriminant
	 *            : Values in the discriminant are guaranteed to satisfy the precondition of the action
	 * @param action
	 *            : Action
	 * @return Probabilistic effect of the action
	 * @throws XMDPException
	 */
	public ProbabilisticEffect formula(Discriminant discriminant, E action) throws XMDPException;
}
