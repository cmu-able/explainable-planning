package explanation.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.NonStandardMetricQFunction;
import language.policy.Policy;

public class QuantitativePolicy implements Iterable<IQFunction<?, ?>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Policy mPolicy;
	private Map<IQFunction<?, ?>, Double> mQAValues = new HashMap<>();
	private Map<NonStandardMetricQFunction<?, ?, ?>, EventBasedQAValue<?>> mEventBasedQAValues = new HashMap<>();

	public QuantitativePolicy(Policy policy) {
		mPolicy = policy;
	}

	public void putQAValue(IQFunction<?, ?> qFunction, double qaValue) {
		mQAValues.put(qFunction, qaValue);
	}

	public <E extends IEvent<?, ?>> void putEventBasedQAValue(NonStandardMetricQFunction<?, ?, E> qFunction,
			EventBasedQAValue<E> qaValue) {
		mEventBasedQAValues.put(qFunction, qaValue);
	}

	public Policy getPolicy() {
		return mPolicy;
	}

	public double getQAValue(IQFunction<?, ?> qFunction) {
		return mQAValues.get(qFunction);
	}

	public <E extends IEvent<?, ?>> EventBasedQAValue<E> getEventBasedQAValue(
			NonStandardMetricQFunction<?, ?, E> qFunction) {
		// Casting: type-safety is ensured in putEventBasedQAValue()
		return (EventBasedQAValue<E>) mEventBasedQAValues.get(qFunction);
	}

	@Override
	public Iterator<IQFunction<?, ?>> iterator() {
		return mQAValues.keySet().iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof QuantitativePolicy)) {
			return false;
		}
		QuantitativePolicy quantPolicy = (QuantitativePolicy) obj;
		return quantPolicy.mPolicy.equals(mPolicy) && quantPolicy.mQAValues.equals(mQAValues)
				&& quantPolicy.mEventBasedQAValues.equals(mEventBasedQAValues);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPolicy.hashCode();
			result = 31 * result + mQAValues.hashCode();
			result = 31 * result + mEventBasedQAValues.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
