package uiconnector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import explanation.analysis.Explanation;
import explanation.analysis.Tradeoff;
import explanation.verbalization.Verbalizer;
import language.policy.Policy;

public class ExplanationWriter {

	private String mExplanationJsonDir;
	private Verbalizer mVerbalizer;

	public ExplanationWriter(String explanationJsonDir, Verbalizer verbalizer) {
		mExplanationJsonDir = explanationJsonDir;
		mVerbalizer = verbalizer;
	}

	public File writeExplanation(String missionName, Explanation explanation, String explanationJsonFilename)
			throws IOException {
		String verbalization = mVerbalizer.verbalize(explanation);

		Policy solutionPolicy = explanation.getSolutionPolicyInfo().getPolicy();
		File solnPolicyJsonFile = mVerbalizer.getPolicyJsonFile(solutionPolicy);

		Set<Tradeoff> tradeoffs = explanation.getTradeoffs();

		JSONArray altPolicyJsonArray = new JSONArray();
		for (Tradeoff tradeoff : tradeoffs) {
			Policy alternativePolicy = tradeoff.getAlternativePolicyInfo().getPolicy();
			File altPolicyJsonFile = mVerbalizer.getPolicyJsonFile(alternativePolicy);
			altPolicyJsonArray.add(altPolicyJsonFile.getAbsolutePath());
		}

		JSONObject explanationJsonObj = new JSONObject();
		explanationJsonObj.put("Mission", missionName);
		explanationJsonObj.put("Solution Policy", solnPolicyJsonFile.getAbsolutePath());
		explanationJsonObj.put("Alternative Policies", altPolicyJsonArray);
		explanationJsonObj.put("Explanation", verbalization);

		File explanationJsonFile = new File(mExplanationJsonDir, explanationJsonFilename);
		try (FileWriter writer = new FileWriter(explanationJsonFile)) {
			writer.write(explanationJsonObj.toJSONString());
			writer.flush();
		}

		return explanationJsonFile;
	}

}
