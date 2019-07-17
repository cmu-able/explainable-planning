package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mobilerobot.study.utilities.ExplanationHTMLGenerator;
import mobilerobot.study.utilities.HTMLGeneratorUtils;
import mobilerobot.study.utilities.HTMLTableSettings;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;

public class PrefAlignQuestionHTMLGenerator {

	private static final String MISSION_TEXT = "Suppose you need to find the best policy from %s to %s that minimizes the following costs:";
	private static final String AGENT_TEXT = "An agent, which may or may not use the same costs as yours, proposes to follow this policy (see \"Agent's Policy\" figure). The expected %s of this policy are as follows:";
	private static final String JSON_EXTENSION = ".json";

	private static final String LEGEND_IMG_FILENAME = "legend.png";
	private static final String LEGEND_SIDEBAR_ID = "legend";
	private static final double LEGEND_WIDTH_PERCENT = 16.0;

	private HTMLTableSettings mTableSettings;
	private ExplanationHTMLGenerator mExplanationHTMLGenerator;

	public PrefAlignQuestionHTMLGenerator(HTMLTableSettings tableSettings) {
		mTableSettings = tableSettings;
		mExplanationHTMLGenerator = new ExplanationHTMLGenerator(mTableSettings);
	}

	public void createPrefAlignQuestionHTMLFile(File questionDir, int agentIndex, boolean withExplanation, File outDir)
			throws IOException, ParseException, URISyntaxException {
		Document questionDoc = createPrefAlignQuestionDocument(questionDir, agentIndex, withExplanation, outDir);
		String questionDocName = QuestionUtils.getPrefAlignQuestionDocumentName(questionDir, agentIndex,
				withExplanation);
		HTMLGeneratorUtils.writeHTMLDocumentToFile(questionDoc, questionDocName, outDir);
	}

	public Document createPrefAlignQuestionDocument(File questionDir, int agentIndex, boolean withExplanation,
			File outDir) throws IOException, ParseException, URISyntaxException {
		// There is only 1 mission[i].json and 1 simpleCostStructure.json per question dir
		JSONObject missionJsonObj = QuestionUtils.getMissionJSONObject(questionDir);
		JSONObject costStructJsonObj = QuestionUtils.getSimpleCostStructureJSONObject(questionDir);

		// Only 1 agentPolicyValues[agentIndex].json and 1 agentPolicy[agentIndex].png per question dir
		File agentPolicyValuesJsonFile = FileIOUtils.listFilesWithContainFilter(questionDir,
				"agentPolicyValues" + agentIndex, JSON_EXTENSION)[0];
		JSONObject agentPolicyQAValuesJsonObj = FileIOUtils.readJSONObjectFromFile(agentPolicyValuesJsonFile);

		File agentPolicyImgFile = FileIOUtils.listFilesWithContainFilter(questionDir, "agentPolicy" + agentIndex,
				".png")[0];

		String instruction = withExplanation
				? "Please scroll down to read the agent's explanation, and answer the following questions:"
				: "Please scroll down to answer the following questions:";

		Element prefAlignQuestionDiv = createPrefAlignQuestionDiv(missionJsonObj, costStructJsonObj,
				agentPolicyQAValuesJsonObj, instruction);
		Element agentPolicyImageDiv = createAgentPolicyImageDiv(agentPolicyImgFile, outDir);

		// Full-screen wrapper container for the task
		Element taskContainer = HTMLGeneratorUtils.createBlankRowContainerFullViewportHeight();
		taskContainer.appendChild(prefAlignQuestionDiv);
		taskContainer.appendChild(agentPolicyImageDiv);

		Document doc = HTMLGeneratorUtils.createHTMLBlankDocument();

		// Collapsible legend
		File legendImgFile = new File(FileIOUtils.getImgsResourceDir(getClass()), LEGEND_IMG_FILENAME);
		addCollapsibleLegend(doc, legendImgFile, outDir);

		// Task
		doc.body().appendChild(taskContainer);

		if (withExplanation) {
			// Explanation
			File agentExplanationDir = new File(questionDir, "explanation-agent" + agentIndex);
			List<Element> explanationElements = mExplanationHTMLGenerator.createExplanationElements(agentExplanationDir,
					outDir);
			for (Element explanationElement : explanationElements) {
				doc.body().appendChild(explanationElement);
			}
		}
		return doc;
	}

	private void addCollapsibleLegend(Document doc, File legendImgFile, File outDir) {
		// To make both paths have the same root
		Path outAbsPath = outDir.toPath().toAbsolutePath();
		Path legendImgAbsPath = legendImgFile.toPath().toAbsolutePath();
		Path legendImgRelativePath = outAbsPath.relativize(legendImgAbsPath);

		Element legendImg = HTMLGeneratorUtils.createResponsiveImg(legendImgRelativePath.toString(), "Legend");
		// Make this image fits the height of the screen with room for "Close" button
		legendImg.attr(HTMLGeneratorUtils.CSS_STYLE, "height:90vh");

		Element sidebar = HTMLGeneratorUtils.createBlankRightSidebar(LEGEND_SIDEBAR_ID, LEGEND_WIDTH_PERCENT);
		sidebar.appendChild(legendImg);

		Element sidebarScript = HTMLGeneratorUtils.createOpenCloseSidebarScript(LEGEND_SIDEBAR_ID);

		doc.body().appendChild(sidebar);
		doc.body().appendChild(sidebarScript);
	}

	private Element createPrefAlignQuestionDiv(JSONObject missionJsonObj, JSONObject costStructJsonObj,
			JSONObject agentPolicyQAValuesJsonObj, String instruction) {
		Element missionDiv = createMissionDiv(missionJsonObj, costStructJsonObj);
		Element agentProposalDiv = createAgentProposalDiv(agentPolicyQAValuesJsonObj);
		Element instructionDiv = HTMLGeneratorUtils.createInstructionContainer(instruction);

		Element container = HTMLGeneratorUtils.createBlankContainer(HTMLGeneratorUtils.W3_HALF);
		container.appendChild(missionDiv);
		container.appendChild(agentProposalDiv);
		container.appendChild(instructionDiv);
		return container;
	}

	private Element createMissionDiv(JSONObject missionJsonObj, JSONObject costStructJsonObj) {
		String startID = (String) missionJsonObj.get("start-id");
		String goalID = (String) missionJsonObj.get("goal-id");

		// Mission paragraph
		String missionInstr = String.format(MISSION_TEXT, startID, goalID);
		Element missionP = new Element("p");
		missionP.text(missionInstr);

		// Cost-structure table
		Element costStructTable = createCostStructureTable(costStructJsonObj);

		Element container = HTMLGeneratorUtils.createBlankContainer();
		container.appendChild(missionP);
		container.appendChild(costStructTable);
		return container;
	}

	private Element createAgentProposalDiv(JSONObject agentPolicyQAValuesJsonObj) {
		// Agent paragraph
		List<String> orderedQANames = mTableSettings.getOrderedQANames();
		String qaListStr = String.join(", ", orderedQANames.subList(0, orderedQANames.size() - 1));
		qaListStr += ", and " + orderedQANames.get(orderedQANames.size() - 1);
		String agentParagraph = String.format(AGENT_TEXT, qaListStr);
		Element agentP = new Element("p");
		agentP.text(agentParagraph);

		// QA values table
		Element qaValuesTableContainer = mExplanationHTMLGenerator
				.createQAValuesTableContainerVertical(agentPolicyQAValuesJsonObj);
		qaValuesTableContainer.selectFirst("table").attr(HTMLGeneratorUtils.CSS_STYLE, "max-width:500px");

		Element container = HTMLGeneratorUtils.createBlankContainer();
		container.appendChild(agentP);
		container.appendChild(qaValuesTableContainer);
		return container;
	}

	private Element createAgentPolicyImageDiv(File agentPolicyImgFile, File outDir) {
		Path agentPolicyImgRelativePath = outDir.toPath().relativize(agentPolicyImgFile.toPath());

		// Agent's policy image
		Element agentPolicyImgDiv = HTMLGeneratorUtils.createResponsiveImgContainer(
				agentPolicyImgRelativePath.toString(), "Agent's Policy", HTMLGeneratorUtils.W3_TWOTHIRD);

		// Legend
		Element legendDiv = HTMLGeneratorUtils.createBlankContainer(HTMLGeneratorUtils.W3_THIRD);
		Element showLegendButton = HTMLGeneratorUtils.createShowRightSidebarButton(LEGEND_SIDEBAR_ID, "Show Legend");
		legendDiv.appendChild(showLegendButton);

		Element container = HTMLGeneratorUtils.createBlankRowContainer(HTMLGeneratorUtils.W3_HALF);
		container.appendChild(agentPolicyImgDiv);
		container.appendChild(legendDiv);
		return container;
	}

	private Element createCostStructureTable(JSONObject costStructJsonObj) {
		Element tableContainer = HTMLGeneratorUtils.createResponsiveBlankTableContainer();
		Element table = tableContainer.selectFirst("table");
		table.attr(HTMLGeneratorUtils.CSS_STYLE, "max-width:400px");
		// Header row
		Element headerRow = table.appendElement("tr");

		// Empty header for quality-attribute unit
		headerRow.appendElement("th");

		// Header for cost
		headerRow.appendElement("th").addClass("w3-right-align").text("Cost ($)");

		for (String qaName : mTableSettings.getOrderedQANames()) {
			JSONObject unitCostJsonObj = (JSONObject) costStructJsonObj.get(qaName);
			String descriptiveUnit = (String) unitCostJsonObj.get("unit");
			String formattedCost = (String) unitCostJsonObj.get("cost");

			// Row for each QA unit cost
			Element qaUnitCostRow = table.appendElement("tr");
			qaUnitCostRow.appendElement("td").text(descriptiveUnit);
			qaUnitCostRow.appendElement("td").addClass("w3-right-align").text("$" + formattedCost);
		}

		return tableContainer;
	}

	public void createAllPrefAlignQuestionHTMLFiles(File rootDir, boolean withExplanation)
			throws IOException, ParseException, URISyntaxException {
		if (QuestionUtils.isQuestionDir(rootDir)) {
			File[] agentPolicyFiles = FileIOUtils.listFilesWithRegexFilter(rootDir, "agentPolicy[0-9]+",
					JSON_EXTENSION);
			for (int i = 0; i < agentPolicyFiles.length; i++) {
				createPrefAlignQuestionHTMLFile(rootDir, i, withExplanation, rootDir);
			}
		} else {
			for (File questionDir : QuestionUtils.listQuestionDirs(rootDir)) {
				createAllPrefAlignQuestionHTMLFiles(questionDir, withExplanation);
			}
		}
	}

	public static void main(String[] args) throws IOException, ParseException, URISyntaxException {
		String pathname = args[0];
		File rootDir = new File(pathname);
		boolean withExplanation = args.length >= 2 && args[1].equals("-e");

		HTMLTableSettings tableSettings = ExplanationHTMLGenerator.getMobileRobotHTMLTableSettings();
		PrefAlignQuestionHTMLGenerator generator = new PrefAlignQuestionHTMLGenerator(tableSettings);
		generator.createAllPrefAlignQuestionHTMLFiles(rootDir, withExplanation);
	}
}
