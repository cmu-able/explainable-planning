package metrics;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNameNotFoundException;
import factors.Transition;

/**
 * {@link CountQFunction} represents a Q_i function that characterizes a QA i by a count of event occurrences.
 * 
 * @author rsukkerd
 *
 */
public class CountQFunction implements IQFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private IEvent mEvent;

	public CountQFunction(IEvent event) {
		mEvent = event;
	}

	@Override
	public double getValue(Transition trans) throws VarNameNotFoundException, AttributeNameNotFoundException {
		return mEvent.isEventOccurred(trans) ? 1 : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CountQFunction)) {
			return false;
		}
		CountQFunction qFun = (CountQFunction) obj;
		return qFun.mEvent.equals(mEvent);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEvent.hashCode();
			hashCode = result;
		}
		return result;
	}
}
