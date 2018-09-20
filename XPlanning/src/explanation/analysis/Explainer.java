package explanation.analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.metrics.IEvent;
import language.metrics.IQFunction;
import language.metrics.NonStandardMetricQFunction;
import language.policy.Policy;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class Explainer {

	private PrismConnectorSettings mConnSettings;

	public Explainer(PrismConnectorSettings prismConnectorSettings) {
		mConnSettings = prismConnectorSettings;
	}

	public Explanation explain(XMDP xmdp, Policy policy) throws PrismException, ResultParsingException, XMDPException,
			IOException, ExplicitModelParsingException, GRBException {
		// PrismConnector
		// Create a new PrismConnector for calculating the QA values of a given policy
		PrismConnector prismConnector = new PrismConnector(xmdp, mConnSettings);
		PolicyInfo solnPolicyInfo = buildPolicyInfo(policy, xmdp.getQSpace(), prismConnector);

		// GRBConnector
		// Export PRISM explicit model files from the XMDP so that GRBConnector can create the corresponding ExplicitMDP
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings, xmdp.getActionSpace());
		GRBConnector grbConnector = new GRBConnector(prismExplicitModelReader);

		AlternativeExplorer altExplorer = new AlternativeExplorer(prismConnector, grbConnector, policy);
		Set<Policy> altPolicies = altExplorer.getParetoOptimalImmediateNeighbors();
		Set<Tradeoff> tradeoffs = new HashSet<>();
		for (Policy altPolicy : altPolicies) {
			PolicyInfo altPolicyInfo = buildPolicyInfo(altPolicy, xmdp.getQSpace(), prismConnector);
			Tradeoff tradeoff = new Tradeoff(solnPolicyInfo, altPolicyInfo, xmdp.getQSpace(), xmdp.getCostFunction());
			tradeoffs.add(tradeoff);
		}
		// Close down PRISM
		prismConnector.terminate();

		return new Explanation(solnPolicyInfo, xmdp.getQSpace(), tradeoffs);
	}

	private PolicyInfo buildPolicyInfo(Policy policy, QSpace qSpace, PrismConnector prismConnector)
			throws ResultParsingException, XMDPException, PrismException {
		PolicyInfo policyInfo = new PolicyInfo(policy);
		for (IQFunction<?, ?> qFunction : qSpace) {
			double qaValue = prismConnector.getQAValue(policy, qFunction);
			policyInfo.putQAValue(qFunction, qaValue);

			if (qFunction instanceof NonStandardMetricQFunction<?, ?, ?>) {
				NonStandardMetricQFunction<?, ?, IEvent<?, ?>> nonStdQFunction = (NonStandardMetricQFunction<?, ?, IEvent<?, ?>>) qFunction;
				EventBasedQAValue<IEvent<?, ?>> eventBasedQAValue = prismConnector.computeEventBasedQAValue(policy,
						nonStdQFunction);
				policyInfo.putEventBasedQAValue(nonStdQFunction, eventBasedQAValue);
			}
		}
		return policyInfo;
	}
}
