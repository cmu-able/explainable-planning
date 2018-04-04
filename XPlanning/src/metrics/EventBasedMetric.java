package metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;

/**
 * {@link EventBasedMetric} is an event-based metric that assigns a specific value to a particular event.
 * 
 * @author rsukkerd
 *
 */
public class EventBasedMetric {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private TransitionDefinition mTransitionDef;
	private Map<IEvent, Double> mMetric;

	public EventBasedMetric(TransitionDefinition transitionDef) {
		mTransitionDef = transitionDef;
		mMetric = new HashMap<>();
	}

	public void put(IEvent event, Double value) {
		mMetric.put(event, value);
	}

	public TransitionDefinition getTransitionDefinition() {
		return mTransitionDef;
	}

	public double getValue(Transition trans) throws VarNotFoundException, AttributeNameNotFoundException {
		for (Entry<IEvent, Double> e : mMetric.entrySet()) {
			IEvent event = e.getKey();
			Double value = e.getValue();
			if (event.hasEventOccurred(trans)) {
				return value;
			}
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof EventBasedMetric)) {
			return false;
		}
		EventBasedMetric metric = (EventBasedMetric) obj;
		return metric.mMetric.equals(mMetric);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMetric.hashCode();
			hashCode = result;
		}
		return result;
	}
}
