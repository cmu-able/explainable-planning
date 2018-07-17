package metrics;

import factors.IAction;

/**
 * {@link ICountQFunction} is an interface to a Q_i function that characterizes a QA i by the number of occurrences of
 * an event.
 * 
 * @author rsukkerd
 *
 */
public interface ICountQFunction<E extends IAction, T extends IQFunctionDomain<E>> extends IQFunction<E, T> {

	public IEvent<E, T> getEvent();
}
