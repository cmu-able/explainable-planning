package models.hmodel;

import explanation.analysis.EventBasedQAValue;
import explanation.analysis.PolicyInfo;
import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.NonStandardMetricQFunction;
import language.mdp.StateVarTuple;

public class QueryPolicyInfo {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarTuple mQueryState;
	private PolicyInfo mQueryPolicyInfo;

	public QueryPolicyInfo(StateVarTuple queryState, PolicyInfo queryPolicyInfo) {
		mQueryState = queryState;
		mQueryPolicyInfo = queryPolicyInfo;
	}

	public StateVarTuple getQueryState() {
		return mQueryState;
	}

	public double getQueryObjectiveCost() {
		return mQueryPolicyInfo.getObjectiveCost();
	}

	public double getQueryQAValue(IQFunction<?, ?> qFunction) {
		return mQueryPolicyInfo.getQAValue(qFunction);
	}

	public <E extends IEvent<?, ?>> EventBasedQAValue<E> getQueryEventBasedQAValue(
			NonStandardMetricQFunction<?, ?, E> qFunction) {
		return mQueryPolicyInfo.getEventBasedQAValue(qFunction);
	}

	public double getQueryScaledQACost(IQFunction<?, ?> qFunction) {
		return mQueryPolicyInfo.getScaledQACost(qFunction);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof QueryPolicyInfo)) {
			return false;
		}
		QueryPolicyInfo queryPolicyInfo = (QueryPolicyInfo) obj;
		return queryPolicyInfo.mQueryState.equals(mQueryState)
				&& queryPolicyInfo.mQueryPolicyInfo.equals(mQueryPolicyInfo);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQueryState.hashCode();
			result = 31 * result + mQueryPolicyInfo.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
