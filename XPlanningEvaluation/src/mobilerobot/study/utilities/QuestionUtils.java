package mobilerobot.study.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.XPlannerOutDirectories;
import explanation.analysis.PolicyInfo;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import mobilerobot.study.prefalign.PrefAlignValidationQuestionGenerator;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import uiconnector.PolicyWriter;

public class QuestionUtils {

	private static final String QUESTION_DOC_NAME_NO_EXPL_FORMAT = "%s-agent%d";
	private static final String QUESTION_DOC_NAME_EXPL_FORMAT = "%s-agent%d-explanation";

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

		XPlannerOutDirectories outputDirs = FileIOUtils.createXPlannerOutDirectories();
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
		File missionJsonFile = getMissionJSONFile(questionDir);
		return FileIOUtils.readJSONObjectFromFile(missionJsonFile);
	}

	public static File getMissionJSONFile(File questionDir) {
		// There is only 1 mission[i].json per question dir
		return FileIOUtils.listFilesWithRegexFilter(questionDir, "mission[0-9]+", JSON_EXTENSION)[0];
	}

	public static JSONObject getScoreCardJSONObject(File questionDir) throws IOException, ParseException {
		// There is only 1 scoreCard.json per question dir
		File scoreCardJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir, "scoreCard", JSON_EXTENSION)[0];
		return FileIOUtils.readJSONObjectFromFile(scoreCardJsonFile);
	}

	public static Set<String> getValidationQuestionDocNames(boolean withExplanation) throws URISyntaxException {
		Set<String> validationQuestionDocNames = new HashSet<>();
		File validationQuestionsRootDir = FileIOUtils.getResourceDir(PrefAlignValidationQuestionGenerator.class,
				"validation-questions");
		for (File questionDir : listQuestionDirs(validationQuestionsRootDir)) {
			FilenameFilter agentPolicyFilenameFilter = (dir, name) -> name.matches("agentPolicy[0-9]+.json");
			int numAgents = questionDir.listFiles(agentPolicyFilenameFilter).length;
			for (int i = 0; i < numAgents; i++) {
				// validationQuestionDocName is the name of the question html file (without .html extension)
				String validationQuestionDocName = String.format(
						withExplanation ? QUESTION_DOC_NAME_EXPL_FORMAT : QUESTION_DOC_NAME_NO_EXPL_FORMAT,
						questionDir.getName(), i);
				validationQuestionDocNames.add(validationQuestionDocName);
			}
		}
		return validationQuestionDocNames;
	}
}
