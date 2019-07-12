package mobilerobot.study.prefalign;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mobilerobot.study.utilities.ExplanationHTMLGenerator;
import mobilerobot.study.utilities.HTMLGeneratorUtils;
import mobilerobot.study.utilities.HTMLTableSettings;
import mobilerobot.study.utilities.MTurkHTMLQuestionUtils;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;

public class PrefAlignQuestionHTMLGenerator {

	private static final String MISSION_TEXT = "Suppose you need to find the best policy from %s to %s that minimizes the following costs:";
	private static final String AGENT_TEXT = "An agent, which may or may not use the same costs as yours, proposes to follow this policy (see \"Agent's Policy\" figure). The expected %s of this policy are as follows:";
	private static final String JSON_EXTENSION = ".json";

	private static final String LEGEND_IMG_PATH = "../../imgs/legend.png";
	private static final String LEGEND_SIDEBAR_ID = "legend";
	private static final double LEGEND_WIDTH_PERCENT = 16.0;

	private HTMLTableSettings mTableSettings;
	private ExplanationHTMLGenerator mExplanationHTMLGenerator;

	public PrefAlignQuestionHTMLGenerator(HTMLTableSettings tableSettings) {
		mTableSettings = tableSettings;
		mExplanationHTMLGenerator = new ExplanationHTMLGenerator(mTableSettings);
	}

	public void createPrefAlignQuestionHTMLFile(File questionDir, int agentIndex, boolean withExplanation)
			throws IOException, ParseException {
		// Only 1 agentPolicy[agentIndex].png and 1 agentPolicyValues[agentIndex].json per question dir
		File agentPolicyPngFile = FileIOUtils.listFilesWithFilter(questionDir, "agentPolicy" + agentIndex, ".png")[0];
		File agentPolicyValuesJsonFile = FileIOUtils.listFilesWithFilter(questionDir, "agentPolicyValues" + agentIndex,
				JSON_EXTENSION)[0];
		JSONObject agentPolicyQAValuesJsonObj = FileIOUtils.readJSONObjectFromFile(agentPolicyValuesJsonFile);

		// There is only 1 mission[i].json and 1 simpleCostStructure.json per question dir
		JSONObject missionJsonObj = QuestionUtils.getMissionJSONObject(questionDir);
		JSONObject costStructJsonObj = QuestionUtils.getSimpleCostStructureJSONObject(questionDir);

		JSONObject explanationJsonObj = null;
		String explanationSrc = "";
		if (withExplanation) {
			// Only 1 mission[j]_explanation.json in explanation-agent[agentIndex] sub-dir
			File agentExplanationDir = new File(questionDir, "explanation-agent" + agentIndex);
			File explanationJsonFile = FileIOUtils.listFilesWithRegexFilter(agentExplanationDir,
					"mission[0-9]+_explanation", JSON_EXTENSION)[0];
			explanationJsonObj = FileIOUtils.readJSONObjectFromFile(explanationJsonFile);
			explanationSrc = agentExplanationDir.getName();
		}

		Document questionDoc = createPrefAlignQuestionDocument(missionJsonObj, costStructJsonObj, agentPolicyPngFile,
				agentPolicyQAValuesJsonObj, explanationJsonObj, explanationSrc);
		String questionDocName = questionDir.getName() + "-agent" + agentIndex;
		if (withExplanation) {
			questionDocName += "-explanation";
		}
		HTMLGeneratorUtils.writeHTMLDocumentToFile(questionDoc, questionDocName, questionDir);
	}

	public Document createPrefAlignQuestionDocument(JSONObject missionJsonObj, JSONObject costStructJsonObj,
			File agentPolicyPngFile, JSONObject agentPolicyQAValuesJsonObj, JSONObject explanationJsonObj,
			String explanationSrc) {
		String instruction = explanationJsonObj == null ? "Please scroll down to answer the following questions:"
				: "Please scroll down to read the agent's explanation, and answer the following questions:";

		Element prefAlignQuestionDiv = createPrefAlignQuestionDiv(missionJsonObj, costStructJsonObj,
				agentPolicyQAValuesJsonObj, instruction);
		Element agentPolicyImageDiv = createAgentPolicyImageDiv(agentPolicyPngFile);

		// Full-screen wrapper container for the task
		Element taskContainer = HTMLGeneratorUtils.createBlankRowContainerFullViewportHeight();
		taskContainer.appendChild(prefAlignQuestionDiv);
		taskContainer.appendChild(agentPolicyImageDiv);

		Document doc = HTMLGeneratorUtils.createHTMLBlankDocument();

		// Collapsible legend
		addCollapsibleLegend(doc);

		// Task
		doc.body().appendChild(taskContainer);

		if (explanationJsonObj != null) {
			// Explanation
			List<Element> explanationElements = mExplanationHTMLGenerator.createExplanationElements(explanationJsonObj,
					explanationSrc);
			for (Element explanationElement : explanationElements) {
				doc.body().appendChild(explanationElement);
			}
		}

		// MTurk Crowd HTML
		Element crowdScript = MTurkHTMLQuestionUtils.getCrowdHTMLScript();
		Element mTurkCrowdFormDiv = createMTurkCrowdFormContainer();

		// FIXME
		int questionIndex = 0;
		int numQuestions = 1;
		String[] dataTypes = new String[] { "answer", "justification", "confidence" };
		Map<String, String[]> dataTypeOptions = new HashMap<>();
		dataTypeOptions.put("answer", new String[] { "yes", "no" });
		dataTypeOptions.put("confidence", new String[] { "high", "medium", "low" });
		Element crowdSubmitScript = MTurkHTMLQuestionUtils.getSubmittableCrowdFormOnSubmitScript(questionIndex,
				numQuestions, dataTypes, dataTypeOptions);

		doc.body().appendChild(crowdScript);
		doc.body().appendChild(mTurkCrowdFormDiv);
		doc.body().appendChild(crowdSubmitScript);
		return doc;
	}

	private void addCollapsibleLegend(Document doc) {
		Element legendImg = HTMLGeneratorUtils.createResponsiveImg(LEGEND_IMG_PATH, "Legend");
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

	private Element createAgentPolicyImageDiv(File agentPolicyPngFile) {
		// Agent's policy image
		Element agentPolicyImgDiv = HTMLGeneratorUtils.createResponsiveImgContainer(agentPolicyPngFile.getName(),
				"Agent's Policy", HTMLGeneratorUtils.W3_TWOTHIRD);

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

	private Element createMTurkCrowdFormContainer() {
		String[] dataTypes = new String[] { "answer", "justification", "confidence" };
		return MTurkHTMLQuestionUtils.createSubmittableCrowdFormContainer(3, dataTypes);
	}

	public void createAllPrefAlignQuestionHTMLFiles(File rootDir, boolean withExplanation)
			throws IOException, ParseException {
		if (rootDir.getName().matches("question-mission[0-9]+")) {
			File[] agentPolicyFiles = FileIOUtils.listFilesWithRegexFilter(rootDir, "agentPolicy[0-9]+",
					JSON_EXTENSION);
			for (int i = 0; i < agentPolicyFiles.length; i++) {
				createPrefAlignQuestionHTMLFile(rootDir, i, withExplanation);
			}
		} else {
			FileFilter dirFilter = File::isDirectory;
			for (File subDir : rootDir.listFiles(dirFilter)) {
				createAllPrefAlignQuestionHTMLFiles(subDir, withExplanation);
			}
		}
	}

	public static void main(String[] args) throws IOException, ParseException {
		String pathname = args[0];
		File rootDir = new File(pathname);
		boolean withExplanation = args.length >= 2 && args[1].equals("-e");

		HTMLTableSettings tableSettings = ExplanationHTMLGenerator.getMobileRobotHTMLTableSettings();
		PrefAlignQuestionHTMLGenerator generator = new PrefAlignQuestionHTMLGenerator(tableSettings);
		generator.createAllPrefAlignQuestionHTMLFiles(rootDir, withExplanation);
	}
}
