package analysis;

import explanation.analysis.Explainer;
import explanation.analysis.PolicyInfo;
import explanation.analysis.Tradeoff;
import language.exceptions.XMDPException;
import models.explanation.HPolicyExplanation;
import models.explanation.HPolicyTag;
import models.explanation.WhyNotQuery;
import models.hmodel.HPolicy;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;

public class HPolicyExplainer {

	private PolicyAnalyzer mPolicyAnalyzer;

	public HPolicyExplainer(PolicyAnalyzer policyAnalyzer) {
		mPolicyAnalyzer = policyAnalyzer;
	}

	public HPolicyExplanation explainHPolicy(PolicyInfo queryPolicyInfo, WhyNotQuery<?, ?> whyNotQuery, HPolicy hPolicy)
			throws ResultParsingException, PrismException, XMDPException {
		PolicyInfo hPolicyInfo = mPolicyAnalyzer.computeHPolicyInfo(hPolicy);
		Tradeoff tradeoff = new Tradeoff(queryPolicyInfo, hPolicyInfo, mPolicyAnalyzer.getXMDP().getQSpace(),
				Explainer.DEFAULT_EQUALITY_TOL);

		HPolicyTag hPolicyTag;

		if (tradeoff.getQAValueGains().containsKey(whyNotQuery.getQueryQFunction())) {
			// HPolicy improves the queried QA
			// By construction, HPolicy is Pareto-optimal -- it worsesn at least 1 other QA
			hPolicyTag = HPolicyTag.BETTER_TARGET_QA;

		} else if (!tradeoff.getQAValueGains().isEmpty()) {
			// HPolicy doesn't improve the queried QA
			// By construction, decision (s_query, a_query) cannot lead to any improvement in the queried QA

			// But, HPolicy improves at least 1 other QA
			// By construction, it is Pareto-optimal

			if (tradeoff.getQAValueLosses().containsKey(whyNotQuery.getQueryQFunction())) {
				// HPolicy worsens the queried QA
				hPolicyTag = HPolicyTag.WORSE_TARGET_QA_NON_DOMINATED;
			} else {
				// HPolicy doesn't affect the queried QA
				hPolicyTag = HPolicyTag.SAME_TARGET_QA_NON_DOMINATED;
			}

		} else if (!tradeoff.getQAValueLosses().isEmpty()) {
			// HPolicy doesn't improve any QA and worsens at least 1 QA
			// HPolicy is dominated

			// The decision (s_query, a_query) leads to a suboptimal policy *** if
			// we act optimally according to the agent's objective function ***

			// But it is possible that there is a non-dominated HPolicy that follows (s_query, a_query)
			// (proved via SMT solver)

			if (tradeoff.getQAValueLosses().containsKey(whyNotQuery.getQueryQFunction())) {
				// HPolicy worsens the queried QA
				hPolicyTag = HPolicyTag.WORSE_TARGET_QA_DOMINATED;
			} else {
				// HPolicy doesn't affect the queried QA
				hPolicyTag = HPolicyTag.SAME_TARGET_QA_DOMINATED;
			}

		} else {
			// HPolicy has the same QA values as the query policy
			hPolicyTag = HPolicyTag.EQUIVALENT;
		}

		return new HPolicyExplanation(hPolicy, hPolicyTag, tradeoff);
	}
}
