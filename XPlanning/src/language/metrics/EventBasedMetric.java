package language.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import language.qfactors.IAction;

/**
 * {@link EventBasedMetric} is an event-based metric that assigns a specific value to a particular event.
 * 
 * @author rsukkerd
 *
 */
public class EventBasedMetric<E extends IAction, T extends IQFunctionDomain<E>, S extends IEvent<E, T>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mName;
	private T mDomain;
	private Map<S, Double> mMetric = new HashMap<>();

	public EventBasedMetric(String name, T domain) {
		mName = name;
		mDomain = domain;
	}

	public void put(S event, double value) {
		mMetric.put(event, value);
	}

	public String getName() {
		return mName;
	}

	public T getQFunctionDomain() {
		return mDomain;
	}

	public double getValue(Transition<E, T> transition) throws VarNotFoundException, AttributeNameNotFoundException {
		for (Entry<S, Double> e : mMetric.entrySet()) {
			S event = e.getKey();
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
		if (!(obj instanceof EventBasedMetric<?, ?, ?>)) {
			return false;
		}
		EventBasedMetric<?, ?, ?> metric = (EventBasedMetric<?, ?, ?>) obj;
		return metric.mName.equals(mName) && metric.mDomain.equals(mDomain) && metric.mMetric.equals(mMetric);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + mMetric.hashCode();
			hashCode = result;
		}
		return result;
	}
}
