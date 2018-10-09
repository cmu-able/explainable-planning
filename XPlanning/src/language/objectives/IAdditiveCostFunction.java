package language.objectives;

import java.util.Set;

import language.domain.models.IAction;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;

/**
 * {@link IAdditiveCostFunction} is an interface to an additive, multi-attribute cost function of n values
 * characterizing n QAs of a single transition in a policy execution.
 * 
 * @author rsukkerd
 *
 */
public interface IAdditiveCostFunction {

	public <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> AttributeCostFunction<S> getAttributeCostFunction(
			S qFunction);

	public double getScalingConstant(AttributeCostFunction<? extends IQFunction<?, ?>> attrCostFunc);

	public String getName();

	public Set<IQFunction<IAction, ITransitionStructure<IAction>>> getQFunctions();
}
