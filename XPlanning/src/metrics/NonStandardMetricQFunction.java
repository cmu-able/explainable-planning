package metrics;

import java.util.Map;

import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;
import factors.Transition;

/**
 * {@link NonStandardMetricQFunction} represents a generic Q_i function that characterizes a QA i using a non-standard
 * metric.
 * 
 * @author rsukkerd
 *
 */
public class NonStandardMetricQFunction implements INonStandardMetricQFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<IEvent, Double> mMetric;

	public NonStandardMetricQFunction(Map<IEvent, Double> metric) {
		mMetric = metric;
	}

	@Override
	public Map<IEvent, Double> getMetric() {
		return mMetric;
	}

	@Override
	public double getValue(Transition trans)
			throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		for (IEvent e : mMetric.keySet()) {
			if (e.hasEventOccurred(trans)) {
				return mMetric.get(e);
			}
		}
		throw new QValueNotFound(trans);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NonStandardMetricQFunction)) {
			return false;
		}
		NonStandardMetricQFunction qFun = (NonStandardMetricQFunction) obj;
		return qFun.mMetric.equals(mMetric);
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
