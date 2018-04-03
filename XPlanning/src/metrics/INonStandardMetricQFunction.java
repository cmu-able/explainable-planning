package metrics;

/**
 * {@link INonStandardMetricQFunction} is an interface to a Q_i function that characterizes a QA i using a non-standard
 * metric.
 * 
 * @author rsukkerd
 *
 */
public interface INonStandardMetricQFunction extends IQFunction {

	public EventBasedMetric getMetric();
}
