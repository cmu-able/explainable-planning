package models.hmodel;

import explanation.analysis.EventBasedQAValue;
import explanation.analysis.PolicyInfo;
import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.NonStandardMetricQFunction;
import language.mdp.StateVarTuple;

public class PartialPolicyInfo {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarTuple mQueryState;
	private PolicyInfo mPartialPolicyInfo;

	public PartialPolicyInfo(StateVarTuple queryState, PolicyInfo partialPolicyInfo) {
		mQueryState = queryState;
		mPartialPolicyInfo = partialPolicyInfo;
	}

	public StateVarTuple getQueryState() {
		return mQueryState;
	}

	public double getPartialObjectiveCost() {
		return mPartialPolicyInfo.getObjectiveCost();
	}

	public double getPartialQAValue(IQFunction<?, ?> qFunction) {
		return mPartialPolicyInfo.getQAValue(qFunction);
	}

	public <E extends IEvent<?, ?>> EventBasedQAValue<E> getPartialEventBasedQAValue(
			NonStandardMetricQFunction<?, ?, E> qFunction) {
		return mPartialPolicyInfo.getEventBasedQAValue(qFunction);
	}

	public double getPartialScaledQACost(IQFunction<?, ?> qFunction) {
		return mPartialPolicyInfo.getScaledQACost(qFunction);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PartialPolicyInfo)) {
			return false;
		}
		PartialPolicyInfo partialPolicyInfo = (PartialPolicyInfo) obj;
		return partialPolicyInfo.mQueryState.equals(mQueryState)
				&& partialPolicyInfo.mPartialPolicyInfo.equals(mPartialPolicyInfo);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQueryState.hashCode();
			result = 31 * result + mPartialPolicyInfo.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
