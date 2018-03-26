package examples.mobilerobot.metrics;

import java.util.HashMap;
import java.util.Map;

import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNotFoundException;
import metrics.IEvent;
import metrics.INonStandardMetricQFunction;
import metrics.NonStandardMetricQFunction;
import metrics.Transition;

/**
 * {@link IntrusivenessQFunction} determines the intrusiveness of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class IntrusivenessQFunction implements INonStandardMetricQFunction {

	private static final double NON_INTRUSIVE_PENALTY = 0;
	private static final double SEMI_INTRUSIVE_PEANLTY = 1;
	private static final double VERY_INTRUSIVE_PENALTY = 3;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private NonStandardMetricQFunction mNonStdQFn;

	public IntrusivenessQFunction() {
		Map<IEvent, Double> metric = new HashMap<>();
		metric.put(new NonIntrusiveMoveEvent(), NON_INTRUSIVE_PENALTY);
		metric.put(new SemiIntrusiveMoveEvent(), SEMI_INTRUSIVE_PEANLTY);
		metric.put(new VeryIntrusiveMoveEvent(), VERY_INTRUSIVE_PENALTY);
		mNonStdQFn = new NonStandardMetricQFunction(metric);
	}

	@Override
	public Map<IEvent, Double> getMetric() {
		return mNonStdQFn.getMetric();
	}

	@Override
	public double getValue(Transition trans)
			throws VarNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		return mNonStdQFn.getValue(trans);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IntrusivenessQFunction)) {
			return false;
		}
		IntrusivenessQFunction qFun = (IntrusivenessQFunction) obj;
		return qFun.mNonStdQFn.equals(mNonStdQFn);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNonStdQFn.hashCode();
			hashCode = result;
		}
		return result;
	}

}
