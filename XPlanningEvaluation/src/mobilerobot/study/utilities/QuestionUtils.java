package mobilerobot.study.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.XPlanningOutDirectories;
import examples.mobilerobot.dsm.parser.JSONSimpleParserUtils;
import explanation.analysis.PolicyInfo;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import uiconnector.PolicyWriter;

public class QuestionUtils {

	private static final String QUESTION_DOC_NAME_NO_EXPL_FORMAT = "%s-agent%d";
	private static final String QUESTION_DOC_NAME_EXPL_FORMAT = "%s-agent%d-explanation";

	private static final String AGENT_KEY_FORMAT = "agentPolicy%d";

	private static final String JSON_EXTENSION = ".json";

	private QuestionUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static File initializeQuestionDir(File missionFile) throws IOException {
		File outputDir = FileIOUtils.getOutputDir();
		String missionName = FilenameUtils.removeExtension(missionFile.getName());

		// Create question directory: /output/question-missionX/
		String questionDirname = "question-" + missionName;
		File questionDir = FileIOUtils.createOutSubDir(outputDir, questionDirname);

		// Copy missionX.json file from /output/missions-of-{mapName}/ to /output/question-missionX/
		// Note: missionX name is unique across all maps
		Files.copy(missionFile.toPath(), questionDir.toPath().resolve(missionFile.getName()));

		return questionDir;
	}

	public static String getPrefAlignQuestionDocumentName(File questionDir, int agentIndex, boolean withExplanation) {
		return String.format(withExplanation ? QUESTION_DOC_NAME_EXPL_FORMAT : QUESTION_DOC_NAME_NO_EXPL_FORMAT,
				questionDir.getName(), agentIndex);
	}

	public static void writeSolutionPolicyToQuestionDir(PolicyInfo solnPolicyInfo, File questionDir)
			throws IOException {
		// Write the solution policy as solnPolicy.json file at /output/question-missionX/
		JSONObject solnPolicyJsonObj = PolicyWriter.writePolicyJSONObject(solnPolicyInfo.getPolicy());
		File solnPolicyFile = FileIOUtils.createOutFile(questionDir, "solnPolicy.json");
		FileIOUtils.prettyPrintJSONObjectToFile(solnPolicyJsonObj, solnPolicyFile);
	}

	public static void writeScoreCardToQuestionDir(JSONObject scoreCardJsonObj, File questionDir) throws IOException {
		// Write the scores of all choice answers as scoreCard.json file at /output/question-missionX/
		File scoreCardFile = FileIOUtils.createOutFile(questionDir, "scoreCard.json");
		FileIOUtils.prettyPrintJSONObjectToFile(scoreCardJsonObj, scoreCardFile);
	}

	public static PrismConnector createPrismConnector(File missionFile, XMDP xmdp) throws IOException, PrismException {
		String missionName = FilenameUtils.removeExtension(missionFile.getName());

		XPlanningOutDirectories outputDirs = FileIOUtils.createXPlanningOutDirectories();
		Path modelOutputPath = outputDirs.getPrismModelsOutputPath().resolve(missionName);
		Path advOutputPath = outputDirs.getPrismAdvsOutputPath().resolve(missionName);

		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath.toString(),
				advOutputPath.toString());
		return new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSetttings);
	}

	public static File[] listQuestionDirs(File rootDir) {
		return rootDir.listFiles(getQuestionDirFileFilter());
	}

	private static FileFilter getQuestionDirFileFilter() {
		return QuestionUtils::isQuestionDir;
	}

	public static boolean isQuestionDir(File file) {
		return file.isDirectory() && file.getName().matches("question-mission[0-9]+");
	}

	public static boolean isExplanationDir(File file) {
		return file.isDirectory() && file.getName().matches("explanation-agent[0-9]+");
	}

	public static JSONObject getSimpleCostStructureJSONObject(File questionDir) throws IOException, ParseException {
		// There is only 1 simpleCostStructure.json per question dir
		File costStructJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir, "simpleCostStructure",
				JSON_EXTENSION)[0];
		return FileIOUtils.readJSONObjectFromFile(costStructJsonFile);
	}

	public static JSONObject getMissionJSONObject(File questionDir) throws IOException, ParseException {
		// There is only 1 mission[i].json per question dir
		File missionJsonFile = FileIOUtils.listFilesWithRegexFilter(questionDir, "mission[0-9]+", JSON_EXTENSION)[0];
		return FileIOUtils.readJSONObjectFromFile(missionJsonFile);
	}

	public static JSONObject getScoreCardJSONObject(File questionDir) throws IOException, ParseException {
		// There is only 1 scoreCard.json per question dir
		File scoreCardJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir, "scoreCard", JSON_EXTENSION)[0];
		return FileIOUtils.readJSONObjectFromFile(scoreCardJsonFile);
	}

	public static JSONObject getExplanationJSONObject(File explanationDir) throws IOException, ParseException {
		// There is only 1 [explanation name].json per explanation dir
		File explanationJsonFile = getExplanationJSONFile(explanationDir);
		return FileIOUtils.readJSONObjectFromFile(explanationJsonFile);
	}

	public static File getExplanationJSONFile(File explanationDir) {
		// There is only 1 [explanation name].json per explanation dir
		return FileIOUtils.listFilesWithContainFilter(explanationDir, "explanation", JSON_EXTENSION)[0];
	}

	public static double getScore(File questionDir, int agentIndex) throws IOException, ParseException {
		// There is only 1 scoreCard.json per question dir
		File scoreCardJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir, "scoreCard", JSON_EXTENSION)[0];
		JSONObject scoreCardJsonObj = FileIOUtils.readJSONObjectFromFile(scoreCardJsonFile);
		String agentKey = String.format(AGENT_KEY_FORMAT, agentIndex);
		return JSONSimpleParserUtils.parseDouble(scoreCardJsonObj.get(agentKey));
	}

	public static String getAnswer(File questionDir, int agentIndex) throws IOException, ParseException {
		// There is only 1 answerKey.json per question dir
		File answerKeyJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir, "answerKey", JSON_EXTENSION)[0];
		JSONObject answerKeyJsonObj = FileIOUtils.readJSONObjectFromFile(answerKeyJsonFile);
		String agentKey = String.format(AGENT_KEY_FORMAT, agentIndex);
		return (String) answerKeyJsonObj.get(agentKey);
	}
}
