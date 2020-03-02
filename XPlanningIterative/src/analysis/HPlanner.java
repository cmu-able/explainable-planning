package analysis;

import java.io.IOException;

import explanation.analysis.AlternativeExplorer;
import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.exceptions.XMDPException;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
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

	public <E extends IAction, T extends ITransitionStructure<E>> HPolicy computeHPolicy(HModel<E> hModel,
			Policy queryPolicy, IQFunction<E, T> queryQFunction) throws PrismException, ExplicitModelParsingException,
			XMDPException, IOException, GRBException, ResultParsingException {
		StateVarTuple queryState = hModel.getQueryState();
		E queryAction = hModel.getQueryAction();
		HPolicy totalHPolicy = new HPolicy(queryPolicy, queryState, queryAction);

		for (StateVarTuple newIniState : hModel.getAllDestStatesOfQuery()) {

			// Query XMDP has one of the resulting states of the why-not query as initial state
			XMDP queryXMDP = hModel.getQueryXMDP(newIniState);

			// Create new PrismConnector with the query state as absorbing state
			PrismConnector prismConnectorForHModel = new PrismConnector(queryXMDP, hModel.getQueryState(),
					mCostCriterion, mPrismConnSettings);

			// Get QA value constraint on alternative policy starting from the new initial state
			double queryQAValueConstraint = hModel.getQueryQAValueConstraint(newIniState, queryQFunction);

			// Compute a constraint-satisfying alternative policy starting from the new initial state
			PolicyInfo partialHPolicy = computePartialHPolicyInfo(prismConnectorForHModel, queryQFunction,
					queryQAValueConstraint);

			// If no QA-constraint-satisfying alternative policy exists, 
			// try to compute alternative policy that satisfies why-not query
			if (partialHPolicy == null) {
				partialHPolicy = prismConnectorForHModel.generateOptimalPolicy();
			}

			// Close down PRISM
			prismConnectorForHModel.terminate();

			// Map each new initial state to partial HPolicy
			totalHPolicy.mapNewInitialStateToPartialHPolicyInfo(newIniState, partialHPolicy);
		}

		return totalHPolicy;
	}

	private PolicyInfo computePartialHPolicyInfo(PrismConnector prismConnectorForHModel,
			IQFunction<?, ?> queryQFunction, double queryQAValueConstraint)
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
				queryQAValueConstraint);
	}

}
