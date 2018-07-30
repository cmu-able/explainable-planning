package analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import exceptions.ResultParsingException;
import exceptions.XMDPException;
import language.policy.Policy;
import mdp.QSpace;
import mdp.XMDP;
import metrics.IQFunction;
import prism.PrismException;
import prismconnector.PrismConnector;
import prismconnector.PrismConnectorSettings;
import verbalization.Verbalizer;
import verbalization.Vocabulary;

public class Explainer {

	private PrismConnectorSettings mConnSettings;
	private Verbalizer mVerbalizer;

	public Explainer(PrismConnectorSettings prismConnectorSettings, Vocabulary vocabulary, String policyJsonDir) {
		mConnSettings = prismConnectorSettings;
		mVerbalizer = new Verbalizer(vocabulary, policyJsonDir);
	}

	public String explain(XMDP xmdp, Policy policy)
			throws PrismException, ResultParsingException, XMDPException, IOException {
		PrismConnector prismConnector = new PrismConnector(xmdp, mConnSettings);
		PolicyInfo solnPolicyInfo = buildPolicyInfo(policy, xmdp.getQSpace(), prismConnector);
		AlternativeExplorer altExplorer = new AlternativeExplorer(prismConnector, policy);
		Set<Policy> altPolicies = altExplorer.getParetoOptimalImmediateNeighbors();
		Set<Tradeoff> tradeoffs = new HashSet<>();
		for (Policy altPolicy : altPolicies) {
			PolicyInfo altPolicyInfo = buildPolicyInfo(altPolicy, xmdp.getQSpace(), prismConnector);
			Tradeoff tradeoff = new Tradeoff(solnPolicyInfo, altPolicyInfo, xmdp.getQSpace(), xmdp.getCostFunction());
			tradeoffs.add(tradeoff);
		}
		return mVerbalizer.verbalize(solnPolicyInfo, xmdp.getQSpace(), tradeoffs);
	}

	private PolicyInfo buildPolicyInfo(Policy policy, QSpace qSpace, PrismConnector prismConnector)
			throws ResultParsingException, XMDPException, PrismException {
		PolicyInfo policyInfo = new PolicyInfo(policy);
		for (IQFunction<?, ?> qFunction : qSpace) {
			double qaValue = prismConnector.getQAValue(policy, qFunction);
			policyInfo.putQAValue(qFunction, qaValue);
		}
		return policyInfo;
	}
}
