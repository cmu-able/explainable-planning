package factors;

import exceptions.ActionNotApplicableException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import mdp.Discriminant;
import mdp.Precondition;
import mdp.ProbabilisticEffect;

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
	 * @param action
	 * @return Probabilistic effect of the action
	 * @throws VarNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws IncompatibleEffectClassException
	 * @throws ActionNotFoundException
	 * @throws ActionNotApplicableException
	 */
	public ProbabilisticEffect formula(Discriminant discriminant, E action)
			throws VarNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			IncompatibleEffectClassException, ActionNotFoundException, ActionNotApplicableException;

	/**
	 * Precondition defining the domains of the discriminant variables.
	 * 
	 * @return Precondition
	 */
	public Precondition<E> getPrecondition();
}
