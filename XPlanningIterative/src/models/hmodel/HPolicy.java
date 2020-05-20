package models.hmodel;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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

	public HPolicy(Policy queryPolicy, Map<StateVarTuple, IAction> preConfigPolicyConstraints,
			StateVarTuple finalQueryState, IAction finalQueryAction) {
		// Initialize total HPolicy to be the query policy (this will be overridden later)
		mTotalHPolicy = new Policy(queryPolicy);

		// Map final s_query to final a_query in total HPolicy
		mTotalHPolicy.put(finalQueryState, finalQueryAction);

		// Map corresponding states in total HPolicy to pre-configuration actions
		for (Entry<StateVarTuple, IAction> e : preConfigPolicyConstraints.entrySet()) {
			mTotalHPolicy.put(e.getKey(), e.getValue());
		}
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

	public void removeResidualDecision(Decision residualDecision) {
		mTotalHPolicy.remove(residualDecision);
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

	/**
	 * Determine whether the query action forces the agent to revisit a state prior to the query state.
	 * 
	 * If so, it means that the total HPolicy, after removing all the residual decisions, does NOT contain the query
	 * state.
	 * 
	 * Note: All residual decisions must be removed via removeResidualDecision() before calling this method.
	 * 
	 * @param queryState
	 *            : Query state
	 * @return Whether the total HPolicy, with all residual decisions removed, does NOT contain the query state
	 */
	public boolean forcesRevisitPriorState(StateVarTuple queryState) {
		return !mTotalHPolicy.containsState(queryState);
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
