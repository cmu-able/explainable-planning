package language.objectives;

/**
 * {@link ILinearCostFunction} is an interface to a linear, single-attribute cost function of the form C(x) = a + b*x,
 * where b > 0. An input value x can be either a total value characterizing a QA of an entire policy execution, or a
 * value of a single transition.
 * 
 * @author rsukkerd
 *
 */
public interface ILinearCostFunction {

	public String getName();

	/**
	 * 
	 * @param value
	 *            : Either a total value characterizing a QA of an entire policy execution, or a value of a single
	 *            transition
	 * @return Single-attribute cost of the value
	 */
	public double getCost(double value);

	/**
	 * 
	 * @param cost
	 *            : Single-attribute cost of either a transition or an entire policy execution
	 * @return Value x = (C(x) - a) / b
	 */
	public double inverse(double cost);
}
