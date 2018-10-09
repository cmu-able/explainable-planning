package language.domain.metrics;

import language.domain.models.IAction;

/**
 * {@link IStandardMetricQFunction} is an interface to a Q_i function that characterizes a QA i using a standard metric.
 * 
 * @author rsukkerd
 *
 */
public interface IStandardMetricQFunction<E extends IAction, T extends ITransitionStructure<E>> extends IQFunction<E, T> {

}
