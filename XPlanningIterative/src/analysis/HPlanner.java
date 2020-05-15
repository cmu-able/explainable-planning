package analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import explanation.analysis.AlternativeExplorer;
import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.domain.models.IAction;
import language.exceptions.XMDPException;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import language.policy.Decision;
import language.policy.Policy;
import models.hmodel.HModel;
import models.hmodel.HPolicy;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.gurobiconnector.GRBConnectorSettings;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class HPlanner {

	private CostCriterion mCostCriterion;
	private PrismConnectorSettings mPrismConnSettings;

	public HPlanner(CostCriterion costCriterion, PrismConnectorSettings prismConnSettings) {
		// Can potentially have HPlanner settings
		mCostCriterion = costCriterion;
		mPrismConnSettings = prismConnSettings;
	}

	public HPolicy computeHPolicy(HModel<? extends IAction> hModel, Policy queryPolicy, IQFunction<?, ?> queryQFunction)
			throws PrismException, ExplicitModelParsingException, XMDPException, IOException, GRBException,
			ResultParsingException {
		// Pre-configuration state-action mapping (deterministic effect only; can be empty)
		Map<StateVarTuple, IAction> preConfigPolicyConstraints = hModel.getPreConfigPolicyConstraints();

		// Final query state and action (can have probabilistic effect)
		StateVarTuple finalQueryState = hModel.getFinalQueryState();
		IAction finalQueryAction = hModel.getFinalQueryAction();

		// HPolicy
		HPolicy hPolicy = new HPolicy(queryPolicy, preConfigPolicyConstraints, finalQueryState, finalQueryAction);

		// All query states (1st query state plus all subsequent states of applying pre-configuration actions)
		// will be made absorbing states
		Set<StateVarTuple> queryStates = new HashSet<>();
		queryStates.addAll(preConfigPolicyConstraints.keySet());
		queryStates.add(finalQueryState);

		for (StateVarTuple newIniState : hModel.getAllDestStatesOfQuery()) {

			// Query XMDP has one of the resulting states of the why-not query as initial state
			XMDP queryXMDP = hModel.getQueryXMDP(newIniState);

			// Create new PrismConnector with the query state(s) as absorbing state(s)
			PrismConnector prismConnectorForHModel = new PrismConnector(queryXMDP, queryStates, mCostCriterion,
					mPrismConnSettings);

			// Get QA value constraint on alternative policy starting from the new initial state
			double qaValueConstraint = hModel.getQAValueConstraint(newIniState, queryQFunction);

			// Compute a constraint-satisfying alternative policy starting from the new initial state
			PolicyInfo partialHPolicy = computePartialHPolicyInfo(prismConnectorForHModel, queryQFunction,
					qaValueConstraint);

			// If no QA-constraint-satisfying alternative policy exists, 
			// try to compute alternative policy that satisfies why-not query
			if (partialHPolicy == null) {
				partialHPolicy = prismConnectorForHModel.generateOptimalPolicy();
			}

			// Close down PRISM
			prismConnectorForHModel.terminate();

			// Map each new initial state to partial HPolicy
			hPolicy.mapNewInitialStateToPartialHPolicyInfo(newIniState, partialHPolicy);
		}

		// Remove all "residual" decisions,
		// whose states are not reachable from the original initial state, following HPolicy
		removeResidualQueryPolicy(hModel.getOriginalXMDP(), queryPolicy, hPolicy);

		return hPolicy;
	}

	private PolicyInfo computePartialHPolicyInfo(PrismConnector prismConnectorForHModel,
			IQFunction<?, ?> queryQFunction, double qaValueConstraint)
			throws XMDPException, PrismException, IOException, ExplicitModelParsingException, GRBException {
		// Query XMDP has one of the resulting states of the why-not query as initial state
		XMDP queryXMDP = prismConnectorForHModel.getXMDP();
		CostCriterion costCriterion = prismConnectorForHModel.getCostCriterion();

		// Use PrismConnector for HModel (with query state to be made absorbing state in the underlying PRISM MDP model)
		// to export PRISM explicit model files from the XMDP,
		// so that GRBConnector can create the corresponding ExplicitMDP
		// ExplicitMDP has the query state as absorbing state
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnectorForHModel.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnectorForHModel.getPrismMDPTranslator().getValueEncodingScheme();
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// GRBConnector
		// To find an alternative policy that satisfies the why-not query and the QA value constraint query
		GRBConnectorSettings grbConnSettings = new GRBConnectorSettings(prismExplicitModelReader);
		GRBConnector grbConnector = new GRBConnector(queryXMDP, costCriterion, grbConnSettings);

		// AlternativeExplorer: use GRBConnector to compute a constraint-satisfying alternative policy on HModel,
		// i.e., satisfying why-not query and improve the query QA
		AlternativeExplorer altExplorer = new AlternativeExplorer(grbConnector);
		return altExplorer.computeHardConstraintSatisfyingAlternative(queryXMDP, queryQFunction,
				qaValueConstraint);
	}

	private void removeResidualQueryPolicy(XMDP originalXMDP, Policy queryPolicy, HPolicy hPolicyWithResidual)
			throws PrismException, ResultParsingException, XMDPException {
		Policy totalHPolicyWithResidual = hPolicyWithResidual.getTotalHPolicy();

		// Create PrismConnector for the original XMDP -- without a query state
		PrismConnector prismConnector = new PrismConnector(originalXMDP, mCostCriterion, mPrismConnSettings);

		// Check each state that is in the query policy,
		// whether it is reachable from the original initial state, following HPolicy
		for (Decision originalDecision : queryPolicy) {
			StateVarTuple originalState = originalDecision.getState();

			double reachProb = prismConnector.computeReachabilityProbability(totalHPolicyWithResidual, originalState);

			// Any state that is not reachable from the original initial state, following HPolicy,
			// is to be removed from HPolicy
			if (reachProb == 0) {
				hPolicyWithResidual.removeResidualDecision(originalDecision);
			}
		}

		// Close down PRISM
		prismConnector.terminate();
	}

}
