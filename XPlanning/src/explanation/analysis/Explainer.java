package explanation.analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class Explainer {

	private PrismConnectorSettings mConnSettings;

	public Explainer(PrismConnectorSettings prismConnectorSettings) {
		mConnSettings = prismConnectorSettings;
	}

	public Explanation explain(XMDP xmdp, CostCriterion costCriterion, PolicyInfo policyInfo)
			throws PrismException, XMDPException, IOException, ExplicitModelParsingException, GRBException {
		// PrismConnector
		// Create a new PrismConnector to export PRISM explicit model files from the XMDP
		// so that GRBConnector can create the corresponding ExplicitMDP
		PrismConnector prismConnector = new PrismConnector(xmdp, costCriterion, mConnSettings);
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);
		// Close down PRISM
		prismConnector.terminate();

		// GRBConnector
		GRBConnector grbConnector = new GRBConnector(xmdp, costCriterion, prismExplicitModelReader);

		AlternativeExplorer altExplorer = new AlternativeExplorer(grbConnector);
		Set<PolicyInfo> altPolicies = altExplorer.getParetoOptimalAlternatives(policyInfo);
		Set<Tradeoff> tradeoffs = new HashSet<>();
		for (PolicyInfo altPolicyInfo : altPolicies) {
			Tradeoff tradeoff = new Tradeoff(policyInfo, altPolicyInfo, xmdp.getQSpace());
			tradeoffs.add(tradeoff);
		}

		return new Explanation(policyInfo, tradeoffs);
	}
}
