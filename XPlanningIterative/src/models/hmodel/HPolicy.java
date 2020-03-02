package models.hmodel;

import java.util.HashMap;
import java.util.Map;

import explanation.analysis.PolicyInfo;
import language.domain.models.IAction;
import language.mdp.StateVarTuple;
import language.policy.Decision;
import language.policy.Policy;

public class HPolicy {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<StateVarTuple, PolicyInfo> mPartialHPolicies = new HashMap<>();

	// Total hypothetical policy -- built from 1 or more partial HPolicies
	private Policy mTotalHPolicy;

	public HPolicy(Policy queryPolicy, StateVarTuple queryState, IAction queryAction) {
		// Initialize total HPolicy to be the query policy (this will be overridden later)
		mTotalHPolicy = new Policy(queryPolicy);

		// In the total HPolicy, a_query is taken in s_query
		mTotalHPolicy.put(queryState, queryAction);
	}

	public void mapNewInitialStateToPartialHPolicyInfo(StateVarTuple newIniState, PolicyInfo partialHPolicyInfo) {
		mPartialHPolicies.put(newIniState, partialHPolicyInfo);

		// Building up total hypothetical policy
		for (Decision decision : partialHPolicyInfo.getPolicy()) {
			StateVarTuple state = decision.getState();
			IAction action = decision.getAction();

			// Add partial HPolicy, starting at each new initial state, to the total HPolicy
			// (override the query policy content)
			mTotalHPolicy.put(state, action);
		}
	}

	public Policy getTotalHPolicy() {
		// Total HPolicy is the query policy overridden by:
		// - (s_query, a_query) decision
		// - Partial HPolicy starting at each new initial state (i.e., each resulting state of s_query, a_query)
		return mTotalHPolicy;
	}

	public PolicyInfo getPartialHPolicyInfo(StateVarTuple newIniState) {
		return mPartialHPolicies.get(newIniState);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HPolicy)) {
			return false;
		}
		HPolicy hPolicy = (HPolicy) obj;
		return hPolicy.mPartialHPolicies.equals(mPartialHPolicies) && hPolicy.mTotalHPolicy.equals(mTotalHPolicy);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPartialHPolicies.hashCode();
			result = 31 * result + mTotalHPolicy.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
