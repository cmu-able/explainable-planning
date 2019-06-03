package explanation.analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import gurobi.GRBException;
import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.NonStandardMetricQFunction;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import language.policy.Policy;
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

public class Explainer {

	private static final double DEFAULT_EQUALITY_TOL = 1e-6;

	private ExplainerSettings mSettings;

	public Explainer(ExplainerSettings settings) {
		mSettings = settings;
	}

	public Explanation explain(XMDP xmdp, CostCriterion costCriterion, PolicyInfo policyInfo) throws PrismException,
			XMDPException, IOException, ExplicitModelParsingException, GRBException, ResultParsingException {
		// PrismConnector
		// Create a new PrismConnector to export PRISM explicit model files from the XMDP
		// so that GRBConnector can create the corresponding ExplicitMDP
		PrismConnectorSettings prismConnSettings = mSettings.getPrismConnectorSettings();
		PrismConnector prismConnector = new PrismConnector(xmdp, costCriterion, prismConnSettings);
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// GRBConnector
		// GRBConnector is used in AlternativeExplorer
		GRBConnectorSettings grbConnSettings = new GRBConnectorSettings(prismExplicitModelReader);
		GRBConnector grbConnector = new GRBConnector(xmdp, costCriterion, grbConnSettings);
		AlternativeExplorer altExplorer = new AlternativeExplorer(grbConnector, mSettings.getDifferenceScaler());
		Set<PolicyInfo> altPolicies = altExplorer.getParetoOptimalAlternatives(policyInfo);

		// Temporary solution: policyInfo might not have its event-based QA values computed yet
		computeEventBasedQAValues(policyInfo, prismConnector);

		Set<Tradeoff> tradeoffs = new HashSet<>();
		for (PolicyInfo altPolicyInfo : altPolicies) {
			// Temporary solution: altPolicyInfo might not have its event-based QA values computed yet
			computeEventBasedQAValues(altPolicyInfo, prismConnector);

			Tradeoff tradeoff = new Tradeoff(policyInfo, altPolicyInfo, xmdp.getQSpace(), DEFAULT_EQUALITY_TOL);
			tradeoffs.add(tradeoff);
		}

		// Close down PRISM
		prismConnector.terminate();

		return new Explanation(policyInfo, tradeoffs);
	}

	/**
	 * Compute the event-based QA values of a given policy, and add the values to the PolicyInfo.
	 * 
	 * This is a temporary solution, since {@link GRBConnector} currently does not compute event-based QA values.
	 * 
	 * @param policyInfo
	 *            : Event-based QA values will be added to this PolicyInfo
	 * @param prismConnector
	 *            : PrismConnector is used to compute the event-based QA values
	 * @throws ResultParsingException
	 * @throws XMDPException
	 * @throws PrismException
	 */
	private void computeEventBasedQAValues(PolicyInfo policyInfo, PrismConnector prismConnector)
			throws ResultParsingException, XMDPException, PrismException {
		Policy policy = policyInfo.getPolicy();

		for (IQFunction<?, ?> qFunction : policyInfo.getXMDP().getQSpace()) {
			if (qFunction instanceof NonStandardMetricQFunction<?, ?, ?>) {
				// Event-based QA value
				NonStandardMetricQFunction<?, ?, IEvent<?, ?>> nonStdQFunction = (NonStandardMetricQFunction<?, ?, IEvent<?, ?>>) qFunction;
				EventBasedQAValue<IEvent<?, ?>> eventBasedQAValue = prismConnector.computeEventBasedQAValue(policy,
						nonStdQFunction);
				policyInfo.putEventBasedQAValue(nonStdQFunction, eventBasedQAValue);
			}
		}
	}
}
