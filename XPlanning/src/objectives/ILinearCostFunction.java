package objectives;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import metrics.Transition;

/**
 * {@link ILinearCostFunction} is an interface to a linear, single-attribute cost function of the form C(x) = a + b*x,
 * where b > 0. An input value x can be either a total value characterizing a QA of an entire policy execution, or a
 * value of a single transition.
 * 
 * @author rsukkerd
 *
 */
public interface ILinearCostFunction {

	public double getCost(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException;

	public double inverse(double cost);
}
