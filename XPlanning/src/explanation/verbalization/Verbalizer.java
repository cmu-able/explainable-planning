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
import language.domain.metrics.EventBasedMetric;
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

	private static final double EQUALITY_THRESHOLD = 5e-4;
	private static final String COMMA_AND = ", and ";

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

		File policyJsonFile = writePolicyToFile(solnPolicyInfo.getPolicy(), "solnPolicy.json");

		StringBuilder builder = new StringBuilder();
		builder.append("I'm planning to follow this policy [");
		builder.append(policyJsonFile.getAbsolutePath());
		builder.append("]. ");
		builder.append(verbalizeQAs(solnPolicyInfo));

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

	public File writePolicyToFile(Policy policy, String policyJsonFilename) throws IOException {
		File policyJsonFile = mPolicyWriter.writePolicy(policy, policyJsonFilename);
		mPolicyJsonFiles.put(policy, policyJsonFile);
		return policyJsonFile;
	}

	public File getPolicyJsonFile(Policy policy) {
		return mPolicyJsonFiles.get(policy);
	}

	public QADecimalFormatter getQADecimalFormatter() {
		return mSettings.getQADecimalFormatter();
	}

	public Vocabulary getVocabulary() {
		return mVocabulary;
	}

	private String verbalizeQAs(PolicyInfo policyInfo) {
		StringBuilder builder = new StringBuilder();
		builder.append("It is expected to ");

		// Describe QAs in a fixed, predefined order
		Iterator<IQFunction<IAction, ITransitionStructure<IAction>>> iter = mSettings
				.getOrderedQFunctions(policyInfo.getXMDP().getQSpace()).iterator();
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

			double qaValue = policyInfo.getQAValue(qFunction);
			double scaledQACost = policyInfo.getScaledQACost(qFunction);

			if (qFunction instanceof NonStandardMetricQFunction<?, ?, ?>) {
				// Nonstandard metric
				// First describe total penalty value
				String formattedPenaltyValue = mSettings.formatQAValue(qFunction, qaValue);
				builder.append("have ");
				builder.append(qFunction.getName());
				builder.append("-penalty of ");
				builder.append(formattedPenaltyValue);
				builder.append(": ");

				// Then describe event-based QA value break-down
				builder.append("it will ");
				builder.append(mVocabulary.getVerb(qFunction.getName()));
				builder.append(" ");

				NonStandardMetricQFunction<?, ?, IEvent<?, ?>> nonStdQFunction = (NonStandardMetricQFunction<?, ?, IEvent<?, ?>>) qFunction;
				EventBasedQAValue<IEvent<?, ?>> eventBasedQAValue = policyInfo.getEventBasedQAValue(nonStdQFunction);
				builder.append(verbalizeEventBasedQAValue(nonStdQFunction, eventBasedQAValue, scaledQACost, false));
			} else {
				// Standard metric or count
				// Use only 1 verb
				builder.append(mVocabulary.getVerb(qFunction.getName()));
				builder.append(" ");
				builder.append(verbalizeQAValue(qFunction, qaValue, scaledQACost, false, false));
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

	private String verbalizeQAValue(IQFunction<?, ?> qFunction, double qaValue, double scaledQACost, boolean isRelative,
			boolean isNounPresent) {
		String formattedQAValue = mSettings.formatQAValue(qFunction, qaValue);
		double roundedQAValue = Double.parseDouble(formattedQAValue);

		StringBuilder builder = new StringBuilder();
		builder.append(formattedQAValue);

		// Include unit of the QA only when the noun is not present, or when it must not be omitted
		if (!isNounPresent || !mVocabulary.omitUnitWhenNounPresent(qFunction.getName())) {
			builder.append(" ");
			builder.append(roundedQAValue > 1 ? mVocabulary.getPluralUnit(qFunction.getName())
					: mVocabulary.getSingularUnit(qFunction.getName()));
		}

		if (mSettings.describeCosts()) {
			builder.append(" ");
			builder.append(verbalizeCost(scaledQACost, isRelative));
		}

		return builder.toString();
	}

	private <E extends IEvent<?, ?>> String verbalizeEventBasedQAValue(
			NonStandardMetricQFunction<?, ?, E> nonStdQFunction, EventBasedQAValue<E> qaValue, double scaledQACost,
			boolean isCostDiff) {
		EventBasedMetric<?, ?, E> eventBasedMetric = nonStdQFunction.getEventBasedMetric();
		StringBuilder builder = new StringBuilder();

		// Describe events of a non-standard QA in a fixed, predefined order
		Iterator<E> iter = mSettings.getOrderedEvents(nonStdQFunction).iterator();
		boolean firstCatValue = true;

		while (iter.hasNext()) {
			E event = iter.next();
			double expectedCount = qaValue.getExpectedCount(event);

			if (firstCatValue) {
				firstCatValue = false;
			} else if (!iter.hasNext()) {
				builder.append(COMMA_AND);
			} else {
				builder.append(", ");
			}

			// Expected number of events
			String formattedExpectedCount = mSettings.formatQAValue(nonStdQFunction, expectedCount);
			double roundedExpectedCount = Double.parseDouble(formattedExpectedCount);

			builder.append(mVocabulary.getCategoricalValue(nonStdQFunction.getName(), event.getName())); // event name
			builder.append(" ");
			builder.append(mVocabulary.getPreposition(nonStdQFunction.getName())); // linking event to measurement unit
			builder.append(" ");
			builder.append(formattedExpectedCount); // number of events
			builder.append(" ");
			builder.append(roundedExpectedCount > 1 ? mVocabulary.getPluralUnit(nonStdQFunction.getName())
					: mVocabulary.getSingularUnit(nonStdQFunction.getName()));

			// Total value from all events
			double eventValue = eventBasedMetric.getEventValue(event);
			double totalEventValue = eventValue * expectedCount;
			String formattedTotalEventValue = mSettings.formatQAValue(nonStdQFunction, totalEventValue);

			builder.append(" (");
			builder.append(formattedTotalEventValue);
			builder.append("-penalty");
			builder.append(")");
		}

		if (mSettings.describeCosts()) {
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

	public String listQAs(Set<IQFunction<IAction, ITransitionStructure<IAction>>> groupQAs) {
		StringBuilder builder = new StringBuilder();
		Iterator<IQFunction<IAction, ITransitionStructure<IAction>>> iter = groupQAs.iterator();
		boolean firstQA = true;
		while (iter.hasNext()) {
			IQFunction<?, ?> qFunction = iter.next();

			if (firstQA) {
				firstQA = false;
			} else if (!iter.hasNext()) {
				builder.append(COMMA_AND);
			} else {
				builder.append(", ");
			}
			builder.append(mVocabulary.getNoun(qFunction.getName()));
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

		File altPolicyJsonFile = writePolicyToFile(alternativePolicy, "altPolicy" + index + ".json");

		StringBuilder builder = new StringBuilder();
		builder.append("Alternatively, following this policy [");
		builder.append(altPolicyJsonFile.getAbsolutePath());
		builder.append("] would ");
		builder.append(verbalizeQADifferences(altPolicyInfo, qaValueGains, qaCostGains));
		builder.append(". ");

		PolicyInfo solnPolicyInfo = tradeoff.getSolutionPolicyInfo();
		double objCostDiff = Math.abs(solnPolicyInfo.getObjectiveCost() - altPolicyInfo.getObjectiveCost());

		if (objCostDiff <= EQUALITY_THRESHOLD) {
			builder.append("It would also ");
			builder.append(verbalizeQADifferences(altPolicyInfo, qaValueLosses, qaCostLosses));
			builder.append(". ");
			builder.append("The objective function is indifferent between this alternative and the solution policy.");
		} else {
			builder.append("However, I didn't choose that policy because it would ");
			builder.append(verbalizeQADifferences(altPolicyInfo, qaValueLosses, qaCostLosses));
			builder.append(". ");
			builder.append(verbalizePreference(qaValueGains, qaValueLosses));
		}

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
				builder.append(COMMA_AND);
			} else {
				builder.append(", ");
			}

			builder.append(diffQAValue < 0 ? "reduce the expected " : "increase the expected ");
			builder.append(mVocabulary.getNoun(qFunction.getName()));

			// Use either relative contrast, or absolute contrast
			double altContrastValue;
			if (mSettings.useRelativeContrast()) {
				altContrastValue = Math.abs(diffQAValue);
				builder.append(" by ");
			} else {
				altContrastValue = altQAValue;
				builder.append(" to ");
			}

			if (qFunction instanceof NonStandardMetricQFunction<?, ?, ?>) {
				// Nonstandard metric
				// First describe total penalty value
				String formattedPenaltyValue = mSettings.formatQAValue(qFunction, altContrastValue);
				builder.append(formattedPenaltyValue);

				// Then describe event-based QA value break-down, but in absolute term
				builder.append(": ");
				builder.append("it would ");
				builder.append(mVocabulary.getVerb(qFunction.getName()));
				builder.append(" ");

				NonStandardMetricQFunction<?, ?, IEvent<?, ?>> nonStdQFunction = (NonStandardMetricQFunction<?, ?, IEvent<?, ?>>) qFunction;
				EventBasedQAValue<IEvent<?, ?>> eventBasedQAValue = altPolicyInfo.getEventBasedQAValue(nonStdQFunction);
				builder.append(verbalizeEventBasedQAValue(nonStdQFunction, eventBasedQAValue, scaledQACostDiff, true));
			} else {
				builder.append(verbalizeQAValue(qFunction, altContrastValue, scaledQACostDiff, true, true));
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

			builder.append(diffQAValue < 0 ? "decrease in expected " : "increase in expected ");
			builder.append(mVocabulary.getNoun(qFunction.getName()));
		}
		return builder.toString();
	}
}
