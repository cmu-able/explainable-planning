package metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import factors.IAction;

/**
 * {@link EventBasedMetric} is an event-based metric that assigns a specific value to a particular event.
 * 
 * @author rsukkerd
 *
 */
public class EventBasedMetric<E extends IAction, T extends IQFunctionDomain<E>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private T mDomain;
	private Map<IEvent<E, T>, Double> mMetric = new HashMap<>();

	public EventBasedMetric(T domain) {
		mDomain = domain;
	}

	public void put(IEvent<E, T> event, Double value) {
		mMetric.put(event, value);
	}

	public T getQFunctionDomain() {
		return mDomain;
	}

	public double getValue(Transition<E, T> transition) throws VarNotFoundException, AttributeNameNotFoundException {
		for (Entry<IEvent<E, T>, Double> e : mMetric.entrySet()) {
			IEvent<E, T> event = e.getKey();
			Double value = e.getValue();
			if (event.hasEventOccurred(transition)) {
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
		if (!(obj instanceof EventBasedMetric<?, ?>)) {
			return false;
		}
		EventBasedMetric<?, ?> metric = (EventBasedMetric<?, ?>) obj;
		return metric.mDomain.equals(mDomain) && metric.mMetric.equals(mMetric);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + mMetric.hashCode();
			hashCode = result;
		}
		return result;
	}
}
