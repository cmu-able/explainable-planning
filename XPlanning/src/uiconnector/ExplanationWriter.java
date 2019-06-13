package uiconnector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import explanation.analysis.EventBasedQAValue;
import explanation.analysis.Explanation;
import explanation.analysis.QuantitativePolicy;
import explanation.analysis.Tradeoff;
import explanation.verbalization.QADecimalFormatter;
import explanation.verbalization.Verbalizer;
import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.NonStandardMetricQFunction;
import language.policy.Policy;

public class ExplanationWriter {

	private File mExplanationJsonDir;
	private Verbalizer mVerbalizer;

	public ExplanationWriter(File explanationJsonDir, Verbalizer verbalizer) {
		mExplanationJsonDir = explanationJsonDir;
		mExplanationJsonDir.mkdirs(); // only make directories when ones don't exist
		mVerbalizer = verbalizer;
	}

	public File writeExplanation(String missionJsonFilename, Explanation explanation, String explanationJsonFilename)
			throws IOException {
		String verbalization = mVerbalizer.verbalize(explanation);

		Policy solutionPolicy = explanation.getSolutionPolicyInfo().getPolicy();
		File solnPolicyJsonFile = mVerbalizer.getPolicyJsonFile(solutionPolicy);

		Set<QuantitativePolicy> allQuantPolicies = new HashSet<>();
		allQuantPolicies.add(explanation.getSolutionPolicyInfo().getQuantitativePolicy());

		Set<Tradeoff> tradeoffs = explanation.getTradeoffs();
		JSONArray altPolicyJsonArray = new JSONArray();
		for (Tradeoff tradeoff : tradeoffs) {
			Policy alternativePolicy = tradeoff.getAlternativePolicyInfo().getPolicy();
			File altPolicyJsonFile = mVerbalizer.getPolicyJsonFile(alternativePolicy);
			altPolicyJsonArray.add(altPolicyJsonFile.getAbsolutePath());

			allQuantPolicies.add(tradeoff.getAlternativePolicyInfo().getQuantitativePolicy());
		}

		JSONObject explanationJsonObj = new JSONObject();
		explanationJsonObj.put("Mission", missionJsonFilename);
		explanationJsonObj.put("Solution Policy", solnPolicyJsonFile.getAbsolutePath());
		explanationJsonObj.put("Alternative Policies", altPolicyJsonArray);
		explanationJsonObj.put("Explanation", verbalization);

		writeQAValuesToExplanationJSONObject(allQuantPolicies, explanationJsonObj);

		File explanationJsonFile = new File(mExplanationJsonDir, explanationJsonFilename);
		try (FileWriter writer = new FileWriter(explanationJsonFile)) {
			writer.write(explanationJsonObj.toJSONString());
			writer.flush();
		}

		return explanationJsonFile;
	}

	private void writeQAValuesToExplanationJSONObject(Set<QuantitativePolicy> quantPolicies,
			JSONObject explanationJsonObj) {
		for (QuantitativePolicy quantPolicy : quantPolicies) {
			File policyJsonFile = mVerbalizer.getPolicyJsonFile(quantPolicy.getPolicy());
			String policyJsonFilename = policyJsonFile.getName();
			JSONObject policyValuesJsonObj = writeQAValuesToJSONObject(quantPolicy,
					mVerbalizer.getQADecimalFormatter());
			explanationJsonObj.put(policyJsonFilename, policyValuesJsonObj);
		}
	}

	public static JSONObject writeQAValuesToJSONObject(QuantitativePolicy quantPolicy,
			QADecimalFormatter decimalFormatter) {
		JSONObject policyValuesJsonObj = new JSONObject();
		for (IQFunction<?, ?> qFunction : quantPolicy) {
			double qaValue = quantPolicy.getQAValue(qFunction);
			String formattedQAValue = decimalFormatter.formatQAValue(qFunction, qaValue);

			if (qFunction instanceof NonStandardMetricQFunction<?, ?, ?>) {
				NonStandardMetricQFunction<?, ?, ?> nonStdQFunction = (NonStandardMetricQFunction<?, ?, ?>) qFunction;
				EventBasedQAValue<?> eventBasedQAValue = quantPolicy.getEventBasedQAValue(nonStdQFunction);

				JSONObject eventBasedValuesJsonObj = new JSONObject();
				for (Entry<? extends IEvent<?, ?>, Double> e : eventBasedQAValue) {
					IEvent<?, ?> event = e.getKey();
					Double expectedCount = e.getValue();
					String formattedExpectedCount = decimalFormatter.formatQAValue(nonStdQFunction, expectedCount);
					eventBasedValuesJsonObj.put(event.getName(), formattedExpectedCount);
				}

				JSONObject nonStdQAValueJsonObj = new JSONObject();
				nonStdQAValueJsonObj.put("Value", formattedQAValue);
				nonStdQAValueJsonObj.put("Event-based Values", eventBasedValuesJsonObj);

				policyValuesJsonObj.put(nonStdQFunction.getName(), nonStdQAValueJsonObj);
			} else {
				policyValuesJsonObj.put(qFunction.getName(), formattedQAValue);
			}
		}
		return policyValuesJsonObj;
	}

}
