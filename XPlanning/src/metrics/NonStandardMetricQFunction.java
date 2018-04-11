package metrics;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;

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

	private String mName;
	private EventBasedMetric mMetric;

	public NonStandardMetricQFunction(String name, EventBasedMetric metric) {
		mName = name;
		mMetric = metric;
	}

	@Override
	public EventBasedMetric getMetric() {
		return mMetric;
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public TransitionDefinition getTransitionDefinition() {
		return mMetric.getTransitionDefinition();
	}

	@Override
	public double getValue(Transition trans) throws VarNotFoundException, AttributeNameNotFoundException {
		return mMetric.getValue(trans);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NonStandardMetricQFunction)) {
			return false;
		}
		NonStandardMetricQFunction qFunc = (NonStandardMetricQFunction) obj;
		return qFunc.mName.equals(mName) && qFunc.mMetric.equals(mMetric);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mMetric.hashCode();
			hashCode = result;
		}
		return result;
	}
}
