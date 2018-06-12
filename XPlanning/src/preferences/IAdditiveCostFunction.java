package preferences;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import metrics.IQFunction;
import metrics.Transition;

/**
 * {@link IAdditiveCostFunction} is an interface to an additive, multi-attribute cost function of n values
 * characterizing n QAs of a single transition in a policy execution.
 * 
 * @author rsukkerd
 *
 */
public interface IAdditiveCostFunction extends Iterable<AttributeCostFunction<IQFunction>> {

	public <E extends IQFunction> AttributeCostFunction<E> getAttributeCostFunction(E qFunction);

	public double getScalingConstant(AttributeCostFunction<? extends IQFunction> attrCostFunc);

	public double getCost(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException;
}
