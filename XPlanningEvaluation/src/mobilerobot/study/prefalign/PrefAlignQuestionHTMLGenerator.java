package mobilerobot.study.prefalign;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mobilerobot.study.utilities.ExplanationHTMLGenerator;
import mobilerobot.study.utilities.HTMLGeneratorUtils;
import mobilerobot.study.utilities.HTMLTableSettings;
import mobilerobot.utilities.FileIOUtils;

public class PrefAlignQuestionHTMLGenerator {

	private static final String MISSION_TEXT = "Suppose you need to find the best policy from %s to %s that minimizes the following costs:";
	private static final String AGENT_TEXT = "An agent, which may or may not use the same costs as yours, proposes to follow this policy (see \"Agent's Policy\" figure). The expected %s of this policy are as follows:";
	private static final String JSON_EXTENSION = ".json";

	private HTMLTableSettings mTableSettings;
	private ExplanationHTMLGenerator mExplanationHTMLGenerator;

	public PrefAlignQuestionHTMLGenerator(HTMLTableSettings tableSettings) {
		mTableSettings = tableSettings;
		mExplanationHTMLGenerator = new ExplanationHTMLGenerator(mTableSettings);
	}

	public void createPrefAlignQuestionHTMLFile(File questionDir, int agentIndex) throws IOException, ParseException {
		// There is only 1 missionX.json and 1 simpleCostStructure.json per question dir
		File missionJsonFile = FileIOUtils.listFilesWithRegexFilter(questionDir, "mission[0-9]+", JSON_EXTENSION)[0];
		File costStructJsonFile = FileIOUtils.listFilesWithFilter(questionDir, "simpleCostStructure",
				JSON_EXTENSION)[0];

		// Only 1 agentPolicy[agentIndex].png and 1 agentPolicyValues[agentIndex].json per question dir
		File agentPolicyPngFile = FileIOUtils.listFilesWithFilter(questionDir, "agentPolicy" + agentIndex, ".png")[0];
		File agentPolicyValuesJsonFile = FileIOUtils.listFilesWithFilter(questionDir, "agentPolicyValues" + agentIndex,
				JSON_EXTENSION)[0];

		JSONObject missionJsonObj = FileIOUtils.readJSONObjectFromFile(missionJsonFile);
		JSONObject costStructJsonObj = FileIOUtils.readJSONObjectFromFile(costStructJsonFile);
		Document questionDoc = createPrefAlignQuestionDocument(missionJsonObj, costStructJsonObj, agentPolicyPngFile,
				agentPolicyValuesJsonFile);
		String questionDocName = questionDir.getName() + "-agent" + agentIndex;
		HTMLGeneratorUtils.writeHTMLDocumentToFile(questionDoc, questionDocName, questionDir);
	}

	private Document createPrefAlignQuestionDocument(JSONObject missionJsonObj, JSONObject costStructJsonObj,
			File agentPolicyPngFile, File agentPolicyValuesJsonFile) throws IOException, ParseException {
		Element prefAlignQuestionDiv = createPrefAlignQuestionDiv(missionJsonObj, costStructJsonObj,
				agentPolicyValuesJsonFile);
		Element agentPolicyImageDiv = createAgentPolicyImageDiv(agentPolicyPngFile);

		// Full-screen wrapper container
		Element container = HTMLGeneratorUtils.createBlankContainerFullViewportHeight();
		container.appendChild(prefAlignQuestionDiv);
		container.appendChild(agentPolicyImageDiv);

		Document doc = HTMLGeneratorUtils.createHTMLBlankDocument();
		doc.body().appendChild(container);
		return doc;
	}

	private Element createPrefAlignQuestionDiv(JSONObject missionJsonObj, JSONObject costStructJsonObj,
			File agentPolicyValuesJsonFile) throws IOException, ParseException {
		Element missionDiv = createMissionDiv(missionJsonObj, costStructJsonObj);
		Element agentProposalDiv = createAgentProposalDiv(agentPolicyValuesJsonFile);

		Element container = HTMLGeneratorUtils.createBlankContainer(HTMLGeneratorUtils.W3_HALF);
		container.appendChild(missionDiv);
		container.appendChild(agentProposalDiv);
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

	private Element createAgentProposalDiv(File agentPolicyValuesJsonFile) throws IOException, ParseException {
		// Agent paragraph
		List<String> orderedQANames = mTableSettings.getOrderedQANames();
		String qaListStr = String.join(", ", orderedQANames.subList(0, orderedQANames.size() - 1));
		qaListStr += ", and " + orderedQANames.get(orderedQANames.size() - 1);
		String agentParagraph = String.format(AGENT_TEXT, qaListStr);
		Element agentP = new Element("p");
		agentP.text(agentParagraph);

		// QA values table
		JSONObject agentPolicyQAValuesJsonObj = FileIOUtils.readJSONObjectFromFile(agentPolicyValuesJsonFile);
		Element qaValuesTableContainer = mExplanationHTMLGenerator
				.createQAValuesTableContainerVertical(agentPolicyQAValuesJsonObj);
		qaValuesTableContainer.selectFirst("table").attr("style", "max-width:500px");

		Element container = HTMLGeneratorUtils.createBlankContainer();
		container.appendChild(agentP);
		container.appendChild(qaValuesTableContainer);
		return container;
	}

	private Element createAgentPolicyImageDiv(File agentPolicyPngFile) {
		// Agent's policy image
		Element agentPolicyImgDiv = HTMLGeneratorUtils.createResponsiveImgContainer(agentPolicyPngFile.getName(),
				"Agent's Policy", HTMLGeneratorUtils.W3_TWOTHIRD);

		// TODO: Add legend

		Element container = HTMLGeneratorUtils.createBlankContainer(HTMLGeneratorUtils.W3_HALF);
		container.appendChild(agentPolicyImgDiv);
		return container;
	}

	private Element createCostStructureTable(JSONObject costStructJsonObj) {
		Element tableContainer = HTMLGeneratorUtils.createResponsiveBlankTableContainer();
		Element table = tableContainer.selectFirst("table");
		table.attr("style", "max-width:400px");
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

	public void createAllPrefAlignQuestionHTMLFiles(File rootDir) throws IOException, ParseException {
		if (rootDir.getName().matches("question-mission[0-9]+")) {
			File[] agentPolicyFiles = FileIOUtils.listFilesWithRegexFilter(rootDir, "agentPolicy[0-9]+",
					JSON_EXTENSION);
			for (int i = 0; i < agentPolicyFiles.length; i++) {
				createPrefAlignQuestionHTMLFile(rootDir, i);
			}
		} else {
			FileFilter dirFilter = File::isDirectory;
			for (File subDir : rootDir.listFiles(dirFilter)) {
				createAllPrefAlignQuestionHTMLFiles(subDir);
			}
		}
	}

	public static void main(String[] args) throws IOException, ParseException {
		String pathname = args[0];
		File rootDir = new File(pathname);

		HTMLTableSettings tableSettings = ExplanationHTMLGenerator.getMobileRobotHTMLTableSettings();
		PrefAlignQuestionHTMLGenerator generator = new PrefAlignQuestionHTMLGenerator(tableSettings);
		generator.createAllPrefAlignQuestionHTMLFiles(rootDir);
	}
}
