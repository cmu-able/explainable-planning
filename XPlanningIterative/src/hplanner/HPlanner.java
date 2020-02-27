package hplanner;

import java.io.IOException;

import explanation.analysis.AlternativeExplorer;
import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.gurobiconnector.GRBConnectorSettings;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class HPlanner {

	public HPlanner() {

	}

	public PolicyInfo computeHPolicy(PrismConnector prismConnectorForHModel, IQFunction<?, ?> queryQFunction,
			double queryQAValueConstraint)
			throws XMDPException, PrismException, IOException, ExplicitModelParsingException, GRBException {
		// XMDP HModel has the resulting state of the why-not query as initial state
		XMDP xmdpHModel = prismConnectorForHModel.getXMDP();
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
		GRBConnector grbConnector = new GRBConnector(xmdpHModel, costCriterion, grbConnSettings);

		// AlternativeExplorer: use GRBConnector to compute a constraint-satisfying alternative policy on HModel,
		// i.e., satisfying why-not query and improve the query QA
		AlternativeExplorer altExplorer = new AlternativeExplorer(grbConnector);
		return altExplorer.computeHardConstraintSatisfyingAlternative(xmdpHModel, queryQFunction,
				queryQAValueConstraint);
	}
}
