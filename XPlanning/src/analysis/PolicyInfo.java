package analysis;

import java.util.HashMap;
import java.util.Map;

import language.policy.Policy;
import metrics.IQFunction;

public class PolicyInfo {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Policy mPolicy;
	private Map<IQFunction<?, ?>, Double> mQAValues = new HashMap<>();

	public PolicyInfo(Policy policy) {
		mPolicy = policy;
	}

	public void putQAValue(IQFunction<?, ?> qFunction, double qaValue) {
		mQAValues.put(qFunction, qaValue);
	}

	public Policy getPolicy() {
		return mPolicy;
	}

	public double getQAValue(IQFunction<?, ?> qFunction) {
		return mQAValues.get(qFunction);
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
		return policyInfo.mPolicy.equals(mPolicy) && policyInfo.mQAValues.equals(mQAValues);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPolicy.hashCode();
			result = 31 * result + mQAValues.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
