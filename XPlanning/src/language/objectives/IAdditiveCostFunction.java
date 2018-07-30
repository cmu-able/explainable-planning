package language.objectives;

import java.util.Set;

import language.metrics.IQFunction;
import language.metrics.IQFunctionDomain;
import language.qfactors.IAction;

/**
 * {@link IAdditiveCostFunction} is an interface to an additive, multi-attribute cost function of n values
 * characterizing n QAs of a single transition in a policy execution.
 * 
 * @author rsukkerd
 *
 */
public interface IAdditiveCostFunction {

	public <E extends IAction, T extends IQFunctionDomain<E>, S extends IQFunction<E, T>> AttributeCostFunction<S> getAttributeCostFunction(
			S qFunction);

	public double getScalingConstant(AttributeCostFunction<? extends IQFunction<?, ?>> attrCostFunc);

	public String getName();

	public Set<IQFunction<IAction, IQFunctionDomain<IAction>>> getQFunctions();
}