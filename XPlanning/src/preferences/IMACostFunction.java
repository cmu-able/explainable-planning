package preferences;

import java.util.Map;

import metrics.IQFunction;

/**
 * {@link IMACostFunction} is an interface to a multi-attribute cost function of n total (i.e., cumulative) values
 * characterizing n QAs of a policy execution.
 * 
 * @author rsukkerd
 *
 */
public interface IMACostFunction {

	/**
	 * 
	 * @param values
	 *            n total (i.e., cumulative) values characterizing n QAs of a policy execution.
	 * @return The cost representing preference on the values.
	 */
	public double getCost(Map<IQFunction, Double> values);
}
