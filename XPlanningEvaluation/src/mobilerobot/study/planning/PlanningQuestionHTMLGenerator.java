package mobilerobot.study.planning;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mobilerobot.study.utilities.HTMLGeneratorUtils;
import mobilerobot.study.utilities.HTMLTableSettings;
import mobilerobot.study.utilities.QuestionUtils;

public class PlanningQuestionHTMLGenerator {

	private static final String MISSION_TEXT = "Find the best policy from %s to %s that minimizes the following costs:";

	private HTMLTableSettings mTableSettings;

	public PlanningQuestionHTMLGenerator(HTMLTableSettings tableSettings) {
		mTableSettings = tableSettings;
	}

	public void createPlanningQuestionHTMLFile(File questionDir) throws IOException, ParseException {
		// There is only 1 mission[i].json and 1 simpleCostStructure.json per question dir
		JSONObject missionJsonObj = QuestionUtils.getMissionJSONObject(questionDir);
		JSONObject costStructJsonObj = QuestionUtils.getSimpleCostStructureJSONObject(questionDir);

		Document questionDoc = createPlanningQuestionDocument(missionJsonObj, costStructJsonObj);
		String questionDocName = questionDir.getName();
		HTMLGeneratorUtils.writeHTMLDocumentToFile(questionDoc, questionDocName, questionDir);
	}

	private Document createPlanningQuestionDocument(JSONObject missionJsonObj, JSONObject costStructJsonObj) {
		Document doc = HTMLGeneratorUtils.createHTMLBlankDocument();

		Element missionQuestionDiv = createMissionQuestionDiv(missionJsonObj, costStructJsonObj);
		doc.body().appendChild(missionQuestionDiv);

		return doc;
	}

	private Element createMissionQuestionDiv(JSONObject missionJsonObj, JSONObject costStructJsonObj) {
		String mapJsonFilename = (String) missionJsonObj.get("map-file");

		// Mission paragraph and cost-structure table
		Element missionDiv = createMissionDiv(missionJsonObj, costStructJsonObj);

		// Map image
		String mapPngFilename = FilenameUtils.removeExtension(mapJsonFilename) + ".png";
		Element mapImgDiv = HTMLGeneratorUtils.createResponsiveImgContainer(mapPngFilename, "Map",
				HTMLGeneratorUtils.W3_HALF);

		Element container = HTMLGeneratorUtils.createBlankRowContainerFullViewportHeight();
		container.appendChild(missionDiv);
		container.appendChild(mapImgDiv);

		return container;
	}

	private Element createMissionDiv(JSONObject missionJsonObj, JSONObject costStructJsonObj) {
		Element container = HTMLGeneratorUtils.createBlankContainer(HTMLGeneratorUtils.W3_HALF);

		String startID = (String) missionJsonObj.get("start-id");
		String goalID = (String) missionJsonObj.get("goal-id");

		// Mission paragraph
		String missionInstr = String.format(MISSION_TEXT, startID, goalID);
		Element missionP = new Element("p");
		missionP.text(missionInstr);

		// Cost-structure table
		Element costStructTable = createCostStructureTable(costStructJsonObj);

		container.appendChild(missionP);
		container.appendChild(costStructTable);

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
}
