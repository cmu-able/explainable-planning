package verbalization;

import java.io.File;
import java.io.IOException;

import explanation.analysis.Tradeoff;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.Vocabulary;
import language.domain.metrics.IQFunction;
import language.domain.models.IAction;
import language.mdp.StateVarTuple;
import models.explanation.HPolicyExplanation;
import models.explanation.HPolicyTag;
import models.explanation.WhyNotQuery;

public class HPolicyVerbalizer {

	private Verbalizer mVerbalizer;
	private Vocabulary mVocabulary;

	public HPolicyVerbalizer(Verbalizer verbalizer) {
		mVerbalizer = verbalizer;
		mVocabulary = verbalizer.getVocabulary();
	}

	public String verbalize(WhyNotQuery<?, ?> whyNotQuery, HPolicyExplanation hPolicyExplanation) throws IOException {
		String whyNotDecisionVP = verbalizeWhyNotDecisionVP(whyNotQuery);
		String queryQAConsequenceSummary = summarizeQueriedQAConsequence(hPolicyExplanation, whyNotQuery);
		File hPolicyJsonFile = mVerbalizer.writePolicyToFile(hPolicyExplanation.getHPolicyInfo().getPolicy(),
				"hPolicy.json");

		StringBuilder builder = new StringBuilder();
		builder.append("To ");
		builder.append(whyNotDecisionVP);
		builder.append(" would ");
		builder.append(queryQAConsequenceSummary);
		builder.append(". ");
		builder.append("To act optimally onwards, I would follow this policy [");
		builder.append(hPolicyJsonFile.getAbsolutePath());
		builder.append("]. ");

		// TODO

		return builder.toString();
	}

	/**
	 * 
	 * @param whyNotQuery
	 * @return Why-not decision verb phrase: take action [action] when in state [state]
	 */
	private String verbalizeWhyNotDecisionVP(WhyNotQuery<?, ?> whyNotQuery) {
		StateVarTuple queryState = whyNotQuery.getQueryState();
		IAction queryAction = whyNotQuery.getQueryAction();

		StringBuilder builder = new StringBuilder();
		builder.append("take action ");
		builder.append(queryAction.toString());
		builder.append(" when in state ");
		builder.append(queryState.toString());
		return builder.toString();
	}

	private String summarizeQueriedQAConsequence(HPolicyExplanation hPolicyExplanation, WhyNotQuery<?, ?> whyNotQuery) {
		HPolicyTag hPolicyTag = hPolicyExplanation.getHPolicyTag();
		Tradeoff tradeoff = hPolicyExplanation.getTradeoff();
		IQFunction<?, ?> queryQFunction = whyNotQuery.getQueryQFunction();

		StringBuilder builder = new StringBuilder();

		if (hPolicyTag == HPolicyTag.BETTER_TARGET_QA) {
			String worseQAs = mVerbalizer.listQAs(tradeoff.getQAValueLosses().keySet());

			builder.append("be able to decrease the ");
			builder.append(mVocabulary.getNoun(queryQFunction.getName()));
			builder.append(" but at the expense of increasing ");
			builder.append(worseQAs);
		} else {
			builder.append("not be able to decrease the ");
			builder.append(mVocabulary.getNoun(queryQFunction.getName()));
		}

		return builder.toString();
	}

}
