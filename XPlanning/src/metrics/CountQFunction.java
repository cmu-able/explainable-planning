package metrics;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNameNotFoundException;

/**
 * {@link CountQFunction} represents a Q_i function that characterizes a QA i by a count of event occurrences.
 * 
 * @author rsukkerd
 *
 */
public class CountQFunction implements IQFunction {

	private IEvent mEvent;
	
	public CountQFunction(IEvent event) {
		mEvent = event;
	}
	
	@Override
	public double getValue(Transition trans) 
			throws VarNameNotFoundException, AttributeNameNotFoundException {
		return mEvent.isEventOccurred(trans) ? 1 : 0;
	}
}
