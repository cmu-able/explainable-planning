package metrics;

/**
 * {@link IStandardMetricQFunction} is an interface to a Q_i function that characterizes a QA i using a standard metric.
 * 
 * @author rsukkerd
 *
 */
public interface IStandardMetricQFunction extends IQFunction {

	public TransitionDefinition getTransitionDefinition();
}
