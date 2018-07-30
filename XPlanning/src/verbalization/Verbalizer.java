package verbalization;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import analysis.PolicyInfo;
import analysis.Tradeoff;
import factors.IAction;
import language.mdp.QSpace;
import language.metrics.IQFunction;
import language.metrics.IQFunctionDomain;
import uiconnector.PolicyWriter;

public class Verbalizer {

	private Vocabulary mVocabulary;
	private PolicyWriter mPolicyWriter;

	public Verbalizer(Vocabulary vocabulary, String policyJsonDir) {
		mVocabulary = vocabulary;
		mPolicyWriter = new PolicyWriter(policyJsonDir);
	}

	public String verbalize(PolicyInfo solnPolicyInfo, QSpace qSpace, Set<Tradeoff> tradeoffs) throws IOException {
		File policyJsonFile = mPolicyWriter.writePolicy(solnPolicyInfo.getPolicy(), "solnPolicy.json");

		StringBuilder builder = new StringBuilder();
		builder.append("I'm planning to follow this policy [");
		builder.append(policyJsonFile.getAbsolutePath());
		builder.append("]. ");
		builder.append(verbalizeQAs(solnPolicyInfo, qSpace));

		int i = 1;
		for (Tradeoff tradeoff : tradeoffs) {
			builder.append("\n\n");
			builder.append(verbalizeTradeoff(tradeoff, i));
			i++;
		}
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

	private String verbalizeTradeoff(Tradeoff tradeoff, int index) throws IOException {
		PolicyInfo altPolicyInfo = tradeoff.getAlternativePolicyInfo();
		Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> qaGains = tradeoff.getQAGains();
		Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> qaLosses = tradeoff.getQALosses();
		File altPolicyJsonFile = mPolicyWriter.writePolicy(altPolicyInfo.getPolicy(), "altPolicy" + index + ".json");

		StringBuilder builder = new StringBuilder();
		builder.append("Alternatively, following this policy [");
		builder.append(altPolicyJsonFile.getAbsolutePath());
		builder.append("] would ");
		builder.append(verbalizeQADifferences(altPolicyInfo, qaGains));
		builder.append(". However, I didn't choose that policy because it would ");
		builder.append(verbalizeQADifferences(altPolicyInfo, qaLosses));
		builder.append(". ");
		builder.append(verbalizePreference(qaGains, qaLosses));
		return builder.toString();
	}

	private String verbalizeQADifferences(PolicyInfo altPolicyInfo,
			Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> qaDiffs) {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<IQFunction<IAction, IQFunctionDomain<IAction>>, Double>> iter = qaDiffs.entrySet().iterator();
		boolean firstQA = true;
		while (iter.hasNext()) {
			Entry<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> e = iter.next();
			IQFunction<?, ?> qFunction = e.getKey();
			double diffQAValue = e.getValue();
			double altQAValue = altPolicyInfo.getQAValue(qFunction);

			if (firstQA) {
				firstQA = false;
			} else if (!iter.hasNext()) {
				builder.append(", and ");
			} else {
				builder.append(", ");
			}

			builder.append(diffQAValue < 0 ? "reduce the " : "increase the ");
			builder.append(mVocabulary.getNoun(qFunction));
			builder.append(" to ");
			builder.append(altQAValue);
			builder.append(" ");
			builder.append(
					altQAValue > 1 ? mVocabulary.getPluralUnit(qFunction) : mVocabulary.getSingularUnit(qFunction));
		}
		return builder.toString();
	}

	private String verbalizePreference(Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> qaGains,
			Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> qaLosses) {
		StringBuilder builder = new StringBuilder();
		builder.append(summarizeQADifferences(qaGains, true));
		builder.append(qaGains.size() > 1 ? " are " : " is ");
		builder.append("not worth ");
		builder.append(summarizeQADifferences(qaLosses, false));
		builder.append(".");
		return builder.toString();
	}

	private String summarizeQADifferences(Map<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> qaDiffs,
			boolean beginSentence) {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<IQFunction<IAction, IQFunctionDomain<IAction>>, Double>> iter = qaDiffs.entrySet().iterator();
		boolean firstQA = true;
		while (iter.hasNext()) {
			Entry<IQFunction<IAction, IQFunctionDomain<IAction>>, Double> e = iter.next();
			IQFunction<?, ?> qFunction = e.getKey();
			double diffQAValue = e.getValue();

			if (firstQA) {
				builder.append(beginSentence ? "The " : "the ");
				firstQA = false;
			} else if (!iter.hasNext()) {
				builder.append(", and the ");
			} else {
				builder.append(", the ");
			}

			builder.append(diffQAValue < 0 ? "decrease in " : "increase in ");
			builder.append(mVocabulary.getNoun(qFunction));
		}
		return builder.toString();
	}
}
