package metrics;

import factors.IAction;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

/**
 * {@link NonStandardMetricQFunction} represents a generic Q_i function that characterizes a QA i using a non-standard
 * metric.
 * 
 * @author rsukkerd
 *
 */
public class NonStandardMetricQFunction<E extends IAction, T extends IQFunctionDomain<E>, S extends IEvent<E, T>>
		implements IQFunction<E, T> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private EventBasedMetric<E, T, S> mMetric;

	public NonStandardMetricQFunction(EventBasedMetric<E, T, S> metric) {
		mMetric = metric;
	}

	public EventBasedMetric<E, T, S> getEventBasedMetric() {
		return mMetric;
	}

	@Override
	public String getName() {
		return mMetric.getName();
	}

	@Override
	public T getQFunctionDomain() {
		return mMetric.getQFunctionDomain();
	}

	@Override
	public double getValue(Transition<E, T> trans) throws VarNotFoundException, AttributeNameNotFoundException {
		return mMetric.getValue(trans);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NonStandardMetricQFunction<?, ?, ?>)) {
			return false;
		}
		NonStandardMetricQFunction<?, ?, ?> qFunction = (NonStandardMetricQFunction<?, ?, ?>) obj;
		return qFunction.mMetric.equals(mMetric);
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
