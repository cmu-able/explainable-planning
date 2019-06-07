package mobilerobot.study.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import examples.common.XPlanningOutDirectories;
import explanation.analysis.PolicyInfo;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import uiconnector.PolicyWriter;

public class QuestionUtils {

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

	public static void createHTMLDocumentExplanation(File explanationJsonFile, File outDir)
			throws IOException, ParseException {
		JSONObject explanationJsonObj = FileIOUtils.readJSONObjectFromFile(explanationJsonFile);
		String explanationText = (String) explanationJsonObj.get("Explanation");
		String explanationWithImages = "";
		String imgHTMLElementStr = "<img src=\"%s\">";

		Pattern jsonFileRefPattern = Pattern.compile("\\[.+\\.json\\]");
		Matcher matcher = jsonFileRefPattern.matcher(explanationText);
		while (matcher.find()) {
			String jsonFileRef = matcher.group(0);
			String imgHTMLElement = String.format(imgHTMLElementStr, jsonFileRef);
			explanationWithImages = explanationText.replace(jsonFileRef, imgHTMLElement);
		}

		Document doc = Jsoup.parse("<html></html>");
		doc.body().appendElement("div");
		Element div = doc.selectFirst("div");
		div.text(explanationWithImages);

		String explanationHTMLStr = doc.toString();
		String explanationHTMLFilename = FilenameUtils.removeExtension(explanationJsonFile.getName()) + ".html";
		Path explanationHTMLPath = outDir.toPath().resolve(explanationHTMLFilename);
		Files.write(explanationHTMLPath, explanationHTMLStr.getBytes());
	}
}
