package explanation.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import language.metrics.IEvent;

public class EventBasedQAValue<E extends IEvent<?, ?>> implements Iterable<Entry<E, Double>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<E, Double> mEventExpectedCounts = new HashMap<>();

	public void putExpectedCount(E event, double expectedCount) {
		mEventExpectedCounts.put(event, expectedCount);
	}

	public double getExpectedCount(E event) {
		return mEventExpectedCounts.get(event);
	}

	@Override
	public Iterator<Entry<E, Double>> iterator() {
		return mEventExpectedCounts.entrySet().iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof EventBasedQAValue<?>)) {
			return false;
		}
		EventBasedQAValue<?> value = (EventBasedQAValue<?>) obj;
		return value.mEventExpectedCounts.equals(mEventExpectedCounts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEventExpectedCounts.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}