package metrics;

import factors.IAction;

/**
 * {@link INonStandardMetricQFunction} is an interface to a Q_i function that characterizes a QA i using a non-standard
 * metric.
 * 
 * @author rsukkerd
 *
 */
public interface INonStandardMetricQFunction<E extends IAction, T extends IQFunctionDomain<E>>
		extends IQFunction<E, T> {

	public EventBasedMetric<E, T> getMetric();
}
