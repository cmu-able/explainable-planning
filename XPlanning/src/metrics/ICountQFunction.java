package metrics;

import exceptions.VarNameNotFoundException;
import mdp.Transition;

/**
 * {@link ICountQFunction} is an interface to a Q_i function that characterizes a QA i by the number of occurrences of
 * an event.
 * 
 * @author rsukkerd
 *
 */
public interface ICountQFunction extends IQFunction {

	public IEvent getEvent();

	public double getValue(Transition trans) throws VarNameNotFoundException;
}
