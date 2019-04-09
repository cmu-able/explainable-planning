package language.objectives;

import java.util.Set;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;

/**
 * {@link IAdditiveCostFunction} is an interface to an additive, multi-attribute cost function of n values
 * characterizing n QAs of a single transition in a policy execution.
 * 
 * {@link IAdditiveCostFunction} may have a positive offset, which is used in formulating SSP to ensure all objective
 * costs are positive, except in the goal states.
 * 
 * @author rsukkerd
 *
 */
public interface IAdditiveCostFunction {

	public <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> AttributeCostFunction<S> getAttributeCostFunction(
			S qFunction);

	public double getScalingConstant(AttributeCostFunction<? extends IQFunction<?, ?>> attrCostFunc);

	public double getOffset();

	public String getName();

	public Set<IQFunction<IAction, ITransitionStructure<IAction>>> getQFunctions();

	public Set<AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>> getAttributeCostFunctions();
}
