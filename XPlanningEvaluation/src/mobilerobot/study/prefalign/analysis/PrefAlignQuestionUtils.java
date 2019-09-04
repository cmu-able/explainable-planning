package mobilerobot.study.prefalign.analysis;

import java.io.File;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import mobilerobot.utilities.FileIOUtils;
import uiconnector.JSONSimpleParserUtils;

public class PrefAlignQuestionUtils {

	private PrefAlignQuestionUtils() {
		throw new IllegalStateException("Utility class");
	}

	private static final String AGENT_KEY_FORMAT = "agentPolicy%d";
	private static final String JSON_EXTENSION = ".json";

	public static double getAgentAlignmentScore(File questionDir, int agentIndex) throws IOException, ParseException {
		// There is only 1 scoreCard.json per question dir
		File scoreCardJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir, "scoreCard", JSON_EXTENSION)[0];
		JSONObject scoreCardJsonObj = FileIOUtils.readJSONObjectFromFile(scoreCardJsonFile);
		String agentKey = String.format(AGENT_KEY_FORMAT, agentIndex);
		return JSONSimpleParserUtils.parseDouble(scoreCardJsonObj.get(agentKey));
	}

	public static String getAgentAlignmentAnswer(File questionDir, int agentIndex) throws IOException, ParseException {
		// There is only 1 answerKey.json per question dir
		File answerKeyJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir, "answerKey", JSON_EXTENSION)[0];
		JSONObject answerKeyJsonObj = FileIOUtils.readJSONObjectFromFile(answerKeyJsonFile);
		String agentKey = String.format(AGENT_KEY_FORMAT, agentIndex);
		return (String) answerKeyJsonObj.get(agentKey);
	}
}
