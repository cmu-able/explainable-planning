package language.domain.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import language.domain.models.IAction;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

/**
 * {@link EventBasedMetric} is an event-based metric that assigns a specific value to a particular event.
 * 
 * @author rsukkerd
 *
 */
public class EventBasedMetric<E extends IAction, T extends ITransitionStructure<E>, S extends IEvent<E, T>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mName;
	private T mTransStructure;
	private Map<S, Double> mMetric = new HashMap<>();

	public EventBasedMetric(String name, T transStructure) {
		mName = name;
		mTransStructure = transStructure;
	}

	public void put(S event, double value) {
		mMetric.put(event, value);
	}

	public String getName() {
		return mName;
	}

	public T getTransitionStructure() {
		return mTransStructure;
	}

	public Set<S> getEvents() {
		return mMetric.keySet();
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
		return metric.mName.equals(mName) && metric.mTransStructure.equals(mTransStructure)
				&& metric.mMetric.equals(mMetric);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mTransStructure.hashCode();
			result = 31 * result + mMetric.hashCode();
			hashCode = result;
		}
		return result;
	}
}
