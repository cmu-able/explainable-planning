package preferences;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import metrics.IQFunction;
import metrics.Transition;

/**
 * {@link IMACostFunction} is an interface to a multi-attribute cost function of n values characterizing n QAs of a
 * single transition in a policy execution.
 * 
 * @author rsukkerd
 *
 */
public interface IMACostFunction {

	public <E extends IQFunction> AttributeCostFunction<E> getAttributeCostFunction(E qFunction);

	public double getScalingConstant(IQFunction qFunction);

	public double getCost(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException;
}
