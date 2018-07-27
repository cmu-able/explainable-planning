package verbalization;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import analysis.PolicyInfo;
import analysis.Tradeoff;
import factors.IAction;
import mdp.QSpace;
import metrics.IQFunction;
import metrics.IQFunctionDomain;
import uiconnector.PolicyWriter;

public class Verbalizer {

	private Vocabulary mVocabulary;
	private PolicyWriter mPolicyWriter;

	public Verbalizer(Vocabulary vocabulary, String policyJsonDir) {
		mVocabulary = vocabulary;
		mPolicyWriter = new PolicyWriter(policyJsonDir);
	}

	public String verbalize(PolicyInfo solnPolicyInfo, Set<PolicyInfo> altPoliciesInfo, QSpace qSpace,
			Tradeoff tradeoff) throws IOException {
		File policyJsonFile = mPolicyWriter.writePolicy(solnPolicyInfo.getPolicy(), "solnPolicy.json");
		int i = 1;
		for (PolicyInfo altPolicyInfo : altPoliciesInfo) {
			mPolicyWriter.writePolicy(altPolicyInfo.getPolicy(), "altPolicy" + i + ".json");
			i++;
		}

		StringBuilder builder = new StringBuilder();
		builder.append("I'm planning to follow this policy [");
		builder.append(policyJsonFile.getAbsolutePath());
		builder.append("]. ");
		builder.append(verbalizeQAs(solnPolicyInfo, qSpace));
		// TODO
		return builder.toString();
	}

	private String verbalizeQAs(PolicyInfo policyInfo, QSpace qSpace) {
		StringBuilder builder = new StringBuilder();
		builder.append("It is expected to ");

		Iterator<IQFunction<IAction, IQFunctionDomain<IAction>>> iter = qSpace.iterator();
		boolean firstQA = true;
		while (iter.hasNext()) {
			IQFunction<?, ?> qFunction = iter.next();

			if (firstQA) {
				firstQA = false;
			} else if (!iter.hasNext()) {
				builder.append(", and ");
			} else {
				builder.append(", ");
			}

			double qaValue = policyInfo.getQAValue(qFunction);
			builder.append(mVocabulary.getVerb(qFunction));
			builder.append(" ");
			builder.append(qaValue);
			builder.append(" ");
			builder.append(qaValue > 1 ? mVocabulary.getPluralUnit(qFunction) : mVocabulary.getSingularUnit(qFunction));
		}
		builder.append(".");
		return builder.toString();
	}
}
