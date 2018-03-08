package preferences;

/**
 * {@link ISACostFunction} is an interface to a single-attribute cost function of a total (i.e., cumulative) value
 * characterizing a QA of a policy execution.
 * 
 * @author rsukkerd
 *
 */
public interface ISACostFunction {

	/**
	 * 
	 * @param value
	 *            A total (i.e., cumulative) value characterizing a QA of a policy execution.
	 * @return The cost representing preference on the value.
	 */
	public double getCost(double value);
}
