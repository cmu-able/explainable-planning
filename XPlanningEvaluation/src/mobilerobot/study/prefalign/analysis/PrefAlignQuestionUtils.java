package mobilerobot.study.prefalign.analysis;

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;
import uiconnector.JSONSimpleParserUtils;

public class PrefAlignQuestionUtils {

	private PrefAlignQuestionUtils() {
		throw new IllegalStateException("Utility class");
	}

	private static final String AGENT_KEY_FORMAT = "agentPolicy%d";
	private static final String JSON_EXTENSION = ".json";

	public static double getAgentAlignmentScore(File questionDir, int agentIndex) throws IOException, ParseException {
		JSONObject scoreCardJsonObj = QuestionUtils.getScoreCardJSONObject(questionDir);
		String agentKey = String.format(AGENT_KEY_FORMAT, agentIndex);
		return JSONSimpleParserUtils.parseDouble(scoreCardJsonObj.get(agentKey));
	}

	public static double getAgentPolicyCost(File questionDir, int agentIndex) throws IOException, ParseException {
		// simpleCostStructure.json
		JSONObject costStructJsonObj = QuestionUtils.getSimpleCostStructureJSONObject(questionDir);

		// agentPolicyValues[i].json
		String agentPolicyValuesName = String.format("agentPolicyValues%d", agentIndex);
		File agentPolicyValuesJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir, agentPolicyValuesName,
				JSON_EXTENSION)[0];
		JSONObject agentPolicyValuesJsonObj = FileIOUtils.readJSONObjectFromFile(agentPolicyValuesJsonFile);

		// Total cost of agent's policy
		double totalCost = 0;
		for (Object key : costStructJsonObj.keySet()) {
			String qaName = (String) key;

			// cost per unit
			JSONObject qaUnitCostJsonObj = (JSONObject) costStructJsonObj.get(qaName);
			String descriptiveUnit = (String) qaUnitCostJsonObj.get("unit");
			String unitCostStr = (String) qaUnitCostJsonObj.get("cost");
			double unit = Double.parseDouble(descriptiveUnit.split(" ")[0]);
			int unitCost = Integer.parseInt(unitCostStr); // every unit cost is an integer 

			// QA value of policy
			Object qaValueObj = agentPolicyValuesJsonObj.get(qaName);
			String qaValueStr;
			if (qaValueObj instanceof JSONObject) {
				JSONObject qaValueJsonObj = (JSONObject) qaValueObj;
				qaValueStr = (String) qaValueJsonObj.get("Value");
			} else {
				qaValueStr = (String) qaValueObj;
			}
			double qaValue = Double.parseDouble(qaValueStr);

			// QA cost of policy
			double qaCost = qaValue / unit * unitCost;

			totalCost += qaCost;
		}

		return totalCost;
	}

	public static String getAgentAlignmentAnswer(File questionDir, int agentIndex) throws IOException, ParseException {
		// There is only 1 answerKey.json per question dir
		File answerKeyJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir, "answerKey", JSON_EXTENSION)[0];
		JSONObject answerKeyJsonObj = FileIOUtils.readJSONObjectFromFile(answerKeyJsonFile);
		String agentKey = String.format(AGENT_KEY_FORMAT, agentIndex);
		return (String) answerKeyJsonObj.get(agentKey);
	}
}
