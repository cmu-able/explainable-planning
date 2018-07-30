package language.metrics;

import factors.IAction;

/**
 * {@link IStandardMetricQFunction} is an interface to a Q_i function that characterizes a QA i using a standard metric.
 * 
 * @author rsukkerd
 *
 */
public interface IStandardMetricQFunction<E extends IAction, T extends IQFunctionDomain<E>> extends IQFunction<E, T> {

}
