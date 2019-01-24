package explanation.analysis;

import java.util.HashMap;
import java.util.Map;

import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.NonStandardMetricQFunction;
import language.mdp.QSpace;
import language.policy.Policy;

public class PolicyInfo {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Policy mPolicy;
	private double mObjectiveCost;
	private QSpace mQSpace = new QSpace();
	private Map<IQFunction<?, ?>, Double> mQAValues = new HashMap<>();
	private Map<NonStandardMetricQFunction<?, ?, ?>, EventBasedQAValue<?>> mEventBasedQAValues = new HashMap<>();
	private Map<IQFunction<?, ?>, Double> mScaledQACosts = new HashMap<>();

	public PolicyInfo(Policy policy, double objectiveCost) {
		mPolicy = policy;
		mObjectiveCost = objectiveCost;
	}

	public void putQAValue(IQFunction<?, ?> qFunction, double qaValue) {
		mQSpace.addQFunction(qFunction);
		mQAValues.put(qFunction, qaValue);
	}

	public <E extends IEvent<?, ?>> void putEventBasedQAValue(NonStandardMetricQFunction<?, ?, E> qFunction,
			EventBasedQAValue<E> qaValue) {
		mEventBasedQAValues.put(qFunction, qaValue);
	}

	public void putScaledQACost(IQFunction<?, ?> qFunction, double scaledQACost) {
		mScaledQACosts.put(qFunction, scaledQACost);
	}

	public Policy getPolicy() {
		return mPolicy;
	}

	public double getObjectiveCost() {
		return mObjectiveCost;
	}

	public QSpace getQSpace() {
		return mQSpace;
	}

	public double getQAValue(IQFunction<?, ?> qFunction) {
		return mQAValues.get(qFunction);
	}

	public <E extends IEvent<?, ?>> EventBasedQAValue<E> getEventBasedQAValue(
			NonStandardMetricQFunction<?, ?, E> qFunction) {
		// Casting: type-safety is ensured in putEventBasedQAValue()
		return (EventBasedQAValue<E>) mEventBasedQAValues.get(qFunction);
	}

	public double getScaledQACost(IQFunction<?, ?> qFunction) {
		return mScaledQACosts.get(qFunction);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PolicyInfo)) {
			return false;
		}
		PolicyInfo policyInfo = (PolicyInfo) obj;
		return policyInfo.mPolicy.equals(mPolicy) && Double.compare(policyInfo.mObjectiveCost, mObjectiveCost) == 0
				&& policyInfo.mQSpace.equals(mQSpace) && policyInfo.mQAValues.equals(mQAValues)
				&& policyInfo.mEventBasedQAValues.equals(mEventBasedQAValues)
				&& policyInfo.mScaledQACosts.equals(mScaledQACosts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPolicy.hashCode();
			result = 31 * result + Double.hashCode(mObjectiveCost);
			result = 31 * result + mQSpace.hashCode();
			result = 31 * result + mQAValues.hashCode();
			result = 31 * result + mEventBasedQAValues.hashCode();
			result = 31 * result + mScaledQACosts.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
