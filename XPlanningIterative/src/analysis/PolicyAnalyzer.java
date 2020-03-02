package analysis;

import explanation.analysis.PolicyInfo;
import language.exceptions.XMDPException;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import language.policy.Policy;
import models.hmodel.PartialPolicyInfo;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.exceptions.ResultParsingException;

public class PolicyAnalyzer {

	private XMDP mXMDP;
	private CostCriterion mCostCriterion;
	private PrismConnectorSettings mPrismConnSettings;

	public PolicyAnalyzer(XMDP xmdp, CostCriterion costCriterion, PrismConnectorSettings prismConnSettings) {
		mXMDP = xmdp;
		mCostCriterion = costCriterion;
		mPrismConnSettings = prismConnSettings;
	}

	public PartialPolicyInfo computePartialPolicyInfo(Policy policy, StateVarTuple queryState)
			throws PrismException, ResultParsingException, XMDPException {
		// Create a new PrismConnector each time a new query state is given
		// Use the query state as initial state to compute QA value

		// Create XMDP model identical to the original model, but with the query state as initial state
		XMDP queryXMDP = new XMDP(mXMDP.getStateSpace(), mXMDP.getActionSpace(), queryState, mXMDP.getGoal(),
				mXMDP.getTransitionFunction(), mXMDP.getQSpace(), mXMDP.getCostFunction());

		// Create Prism connector (without the query state as absorbing state)
		PrismConnector prismConnector = new PrismConnector(queryXMDP, mCostCriterion, mPrismConnSettings);

		// Compute QA values, objective cost, scaled QA costs of the policy, starting from the query state
		PolicyInfo policyInfo = prismConnector.buildPolicyInfo(policy);

		PartialPolicyInfo partialPolicyInfo = new PartialPolicyInfo(queryState, policyInfo);

		// Close down PRISM
		prismConnector.terminate();

		return partialPolicyInfo;
	}

	public XMDP getXMDP() {
		return mXMDP;
	}
}
