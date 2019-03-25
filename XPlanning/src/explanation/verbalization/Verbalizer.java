package explanation.verbalization;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import explanation.analysis.EventBasedQAValue;
import explanation.analysis.Explanation;
import explanation.analysis.PolicyInfo;
import explanation.analysis.Tradeoff;
import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.NonStandardMetricQFunction;
import language.domain.models.IAction;
import language.mdp.QSpace;
import language.objectives.AttributeCostFunction;
import language.objectives.CostCriterion;
import language.objectives.CostFunction;
import language.policy.Policy;
import uiconnector.PolicyWriter;

public class Verbalizer {

	private Vocabulary mVocabulary;
	private CostCriterion mCostCriterion;
	private PolicyWriter mPolicyWriter;
	private Map<Policy, File> mPolicyJsonFiles = new HashMap<>();
	private VerbalizerSettings mSettings;

	public Verbalizer(Vocabulary vocabulary, CostCriterion costCriterion, File policyJsonDir,
			VerbalizerSettings settings) {
		mVocabulary = vocabulary;
		mCostCriterion = costCriterion;
		mPolicyWriter = new PolicyWriter(policyJsonDir);
		mSettings = settings;
	}

	public String verbalize(Explanation explanation) throws IOException {
		PolicyInfo solnPolicyInfo = explanation.getSolutionPolicyInfo();
		QSpace qSpace = explanation.getQSpace();
		CostFunction costFunction = explanation.getCostFunction();
		Set<Tradeoff> tradeoffs = explanation.getTradeoffs();

		String policyJsonFilename = "solnPolicy.json";
		Policy solutionPolicy = solnPolicyInfo.getPolicy();
		File policyJsonFile = mPolicyWriter.writePolicy(solutionPolicy, policyJsonFilename);
		mPolicyJsonFiles.put(solutionPolicy, policyJsonFile);

		StringBuilder builder = new StringBuilder();
		builder.append("I'm planning to follow this policy [");
		builder.append(policyJsonFile.getAbsolutePath());
		builder.append("]. ");
		builder.append(verbalizeQAs(solnPolicyInfo, qSpace));

		// Optimal QAs can have either the lowest values (when attribute cost function has positive slope) or the
		// highest values (when attribute cost function has negative slope).
		Set<IQFunction<IAction, ITransitionStructure<IAction>>> lowestOptimalQAs = new HashSet<>();
		Set<IQFunction<IAction, ITransitionStructure<IAction>>> highestOptimalQAs = new HashSet<>();
		getOptimalQAs(qSpace, costFunction, tradeoffs, lowestOptimalQAs, highestOptimalQAs);

		if (!lowestOptimalQAs.isEmpty() || !highestOptimalQAs.isEmpty()) {
			builder.append(" ");
			builder.append(verbalizeOptimalQAValues(lowestOptimalQAs, highestOptimalQAs));
		}

		int i = 1;
		for (Tradeoff tradeoff : tradeoffs) {
			builder.append("\n\n");
			builder.append(verbalizeTradeoff(tradeoff, i));
			i++;
		}
		return builder.toString();
	}

	public File getPolicyJsonFile(Policy policy) {
		return mPolicyJsonFiles.get(policy);
	}

	private String verbalizeQAs(PolicyInfo policyInfo, QSpace qSpace) {
		StringBuilder builder = new StringBuilder();
		builder.append("It is expected to ");

		Iterator<IQFunction<IAction, ITransitionStructure<IAction>>> iter = qSpace.iterator();
		boolean firstQA = true;
		while (iter.hasNext()) {
			IQFunction<?, ?> qFunction = iter.next();

			if (firstQA) {
				firstQA = false;
			} else if (!iter.hasNext()) {
				builder.append("; and ");
			} else {
				builder.append("; ");
			}

			builder.append(mVocabulary.getVerb(qFunction));
			builder.append(" ");

			if (qFunction instanceof NonStandardMetricQFunction<?, ?, ?>) {
				NonStandardMetricQFunction<?, ?, IEvent<?, ?>> nonStdQFunction = (NonStandardMetricQFunction<?, ?, IEvent<?, ?>>) qFunction;
				EventBasedQAValue<IEvent<?, ?>> eventBasedQAValue = policyInfo.getEventBasedQAValue(nonStdQFunction);
				double scaledQACost = policyInfo.getScaledQACost(nonStdQFunction);
				builder.append(verbalizeEventBasedQAValue(nonStdQFunction, eventBasedQAValue, scaledQACost, false));
			} else {
				double qaValue = policyInfo.getQAValue(qFunction);
				double scaledQACost = policyInfo.getScaledQACost(qFunction);
				builder.append(verbalizeQAValue(qFunction, qaValue, scaledQACost, false));
			}
		}

		if (mCostCriterion == CostCriterion.AVERAGE_COST) {
			builder.append(" per ");
			builder.append(mVocabulary.getPeriodUnit());
			builder.append(" on average.");
		} else {
			builder.append(".");
		}
		return builder.toString();
	}

	private String verbalizeQAValue(IQFunction<?, ?> qFunction, double qaValue, double scaledQACost,
			boolean isCostDiff) {
		StringBuilder builder = new StringBuilder();
		builder.append(qaValue);
		builder.append(" ");
		builder.append(qaValue > 1 ? mVocabulary.getPluralUnit(qFunction) : mVocabulary.getSingularUnit(qFunction));

		if (mSettings.getDescribeCosts()) {
			builder.append(" ");
			builder.append(verbalizeCost(scaledQACost, isCostDiff));
		}

		return builder.toString();
	}

	private <E extends IEvent<?, ?>> String verbalizeEventBasedQAValue(NonStandardMetricQFunction<?, ?, E> qFunction,
			EventBasedQAValue<E> qaValue, double scaledQACost, boolean isCostDiff) {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<E, Double>> iter = qaValue.iterator();
		boolean firstCatValue = true;
		while (iter.hasNext()) {
			Entry<E, Double> entry = iter.next();

			if (firstCatValue) {
				firstCatValue = false;
			} else if (!iter.hasNext()) {
				builder.append(", and ");
			} else {
				builder.append(", ");
			}

			E event = entry.getKey();
			double expectedCount = entry.getValue();
			builder.append(mVocabulary.getCategoricalValue(qFunction, event));
			builder.append(" ");
			builder.append(expectedCount);
			builder.append(" ");
			builder.append(
					expectedCount > 1 ? mVocabulary.getPluralUnit(qFunction) : mVocabulary.getSingularUnit(qFunction));
		}

		if (mSettings.getDescribeCosts()) {
			builder.append(" ");
			builder.append(verbalizeCost(scaledQACost, isCostDiff));
		}

		return builder.toString();
	}

	private String verbalizeCost(double scaledQACost, boolean isCostDiff) {
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		if (isCostDiff) {
			builder.append(scaledQACost >= 0 ? "+" : "");
		}
		builder.append(scaledQACost);
		builder.append(" in cost");
		builder.append(")");
		return builder.toString();
	}

	/**
	 * Get a set of lowest-value optimal QAs and a set of highest-value optimal QAs. The two sets are mutually
	 * exclusive.
	 * 
	 * @param qSpace
	 * @param costFunction
	 * @param tradeoffs
	 * @param lowestOptimalQAs
	 *            : return parameter
	 * @param highestOptimalQAs
	 *            : return parameter
	 */
	private void getOptimalQAs(QSpace qSpace, CostFunction costFunction, Set<Tradeoff> tradeoffs,
			Set<IQFunction<IAction, ITransitionStructure<IAction>>> lowestOptimalQAs,
			Set<IQFunction<IAction, ITransitionStructure<IAction>>> highestOptimalQAs) {
		for (IQFunction<IAction, ITransitionStructure<IAction>> qFunction : qSpace) {
			AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> attrCostFunc = costFunction
					.getAttributeCostFunction(qFunction);

			boolean isOptimal = true; // if there is no tradeoff, then all QAs have optimal values
			boolean negativeSlope = attrCostFunc.getSlope() < 0;

			for (Tradeoff tradeoff : tradeoffs) {
				// Check each alternative in the tradeoff set if it has an improvement on this QA.
				// If so, then this QA is not optimal.
				if (tradeoff.getQAValueGains().containsKey(qFunction)) {
					isOptimal = false;
					break;
				}
			}

			if (isOptimal && negativeSlope) {
				// Optimal value of this QA has the highest value
				highestOptimalQAs.add(qFunction);
			} else if (isOptimal) {
				// Optimal value of this QA has the lowest value
				lowestOptimalQAs.add(qFunction);
			}
		}
	}

	private String verbalizeOptimalQAValues(Set<IQFunction<IAction, ITransitionStructure<IAction>>> lowestOptimalQAs,
			Set<IQFunction<IAction, ITransitionStructure<IAction>>> highestOptimalQAs) {
		StringBuilder builder = new StringBuilder();
		boolean beginSentence = true;

		if (!lowestOptimalQAs.isEmpty()) {
			String listOfLowestOptimalQAs = listQAs(lowestOptimalQAs);
			builder.append("It has the lowest expected ");
			builder.append(listOfLowestOptimalQAs);
			beginSentence = false;
		}

		if (!highestOptimalQAs.isEmpty()) {
			String listOfHighestOptimalQAs = listQAs(highestOptimalQAs);
			if (beginSentence) {
				builder.append("It has the highest expected ");
			} else {
				builder.append("; and has the highest expected ");
			}
			builder.append(listOfHighestOptimalQAs);
		}

		builder.append(".");
		return builder.toString();
	}

	private String listQAs(Set<IQFunction<IAction, ITransitionStructure<IAction>>> optimalQAs) {
		StringBuilder builder = new StringBuilder();
		Iterator<IQFunction<IAction, ITransitionStructure<IAction>>> iter = optimalQAs.iterator();
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
			builder.append(mVocabulary.getNoun(qFunction));
		}
		return builder.toString();
	}

	private String verbalizeTradeoff(Tradeoff tradeoff, int index) throws IOException {
		PolicyInfo altPolicyInfo = tradeoff.getAlternativePolicyInfo();
		Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> qaValueGains = tradeoff.getQAValueGains();
		Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> qaCostGains = tradeoff.getQACostGains();
		Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> qaValueLosses = tradeoff.getQAValueLosses();
		Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> qaCostLosses = tradeoff.getQACostLosses();
		Policy alternativePolicy = altPolicyInfo.getPolicy();
		File altPolicyJsonFile = mPolicyWriter.writePolicy(alternativePolicy, "altPolicy" + index + ".json");
		mPolicyJsonFiles.put(alternativePolicy, altPolicyJsonFile);

		StringBuilder builder = new StringBuilder();
		builder.append("Alternatively, following this policy [");
		builder.append(altPolicyJsonFile.getAbsolutePath());
		builder.append("] would ");
		builder.append(verbalizeQADifferences(altPolicyInfo, qaValueGains, qaCostGains));
		builder.append(". However, I didn't choose that policy because it would ");
		builder.append(verbalizeQADifferences(altPolicyInfo, qaValueLosses, qaCostLosses));
		builder.append(". ");
		builder.append(verbalizePreference(qaValueGains, qaValueLosses));
		return builder.toString();
	}

	private String verbalizeQADifferences(PolicyInfo altPolicyInfo,
			Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> qaDiffs,
			Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> scaledQACostDiffs) {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<IQFunction<IAction, ITransitionStructure<IAction>>, Double>> iter = qaDiffs.entrySet()
				.iterator();
		boolean firstQA = true;
		while (iter.hasNext()) {
			Entry<IQFunction<IAction, ITransitionStructure<IAction>>, Double> e = iter.next();
			IQFunction<?, ?> qFunction = e.getKey();
			double diffQAValue = e.getValue(); // Difference in QA values
			double scaledQACostDiff = scaledQACostDiffs.get(qFunction); // Difference in scaled QA costs
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

			if (qFunction instanceof NonStandardMetricQFunction<?, ?, ?>) {
				NonStandardMetricQFunction<?, ?, IEvent<?, ?>> nonStdQFunction = (NonStandardMetricQFunction<?, ?, IEvent<?, ?>>) qFunction;
				EventBasedQAValue<IEvent<?, ?>> eventBasedQAValue = altPolicyInfo.getEventBasedQAValue(nonStdQFunction);
				builder.append(verbalizeEventBasedQAValue(nonStdQFunction, eventBasedQAValue, scaledQACostDiff, true));
			} else {
				builder.append(verbalizeQAValue(qFunction, altQAValue, scaledQACostDiff, true));
			}
		}
		return builder.toString();
	}

	private String verbalizePreference(Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> qaGains,
			Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> qaLosses) {
		StringBuilder builder = new StringBuilder();
		builder.append(summarizeQADifferences(qaGains, true));
		builder.append(qaGains.size() > 1 ? " are " : " is ");
		builder.append("not worth ");
		builder.append(summarizeQADifferences(qaLosses, false));
		builder.append(".");
		return builder.toString();
	}

	private String summarizeQADifferences(Map<IQFunction<IAction, ITransitionStructure<IAction>>, Double> qaDiffs,
			boolean beginSentence) {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<IQFunction<IAction, ITransitionStructure<IAction>>, Double>> iter = qaDiffs.entrySet()
				.iterator();
		boolean firstQA = true;
		while (iter.hasNext()) {
			Entry<IQFunction<IAction, ITransitionStructure<IAction>>, Double> e = iter.next();
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
