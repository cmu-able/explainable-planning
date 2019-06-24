package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import mobilerobot.study.utilities.HTMLGeneratorUtils;
import mobilerobot.utilities.FileIOUtils;

public class PrefAlignQuestionHTMLGenerator {

	private static final String MISSION_TEXT = "Find the best policy from %s to %s that minimizes the following costs:";
	private static final String JSON_EXTENSION = ".json";

	public PrefAlignQuestionHTMLGenerator() {
		//
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
		String questionDocName = null;
		HTMLGeneratorUtils.writeHTMLDocumentToFile(questionDoc, questionDocName, questionDir);
	}

	private Document createPrefAlignQuestionDocument(JSONObject missionJsonObj, JSONObject costStructJsonObj,
			File agentPolicyPngFile, File agentPolicyValuesJsonFile) {
		Document doc = HTMLGeneratorUtils.createHTMLBlankDocument();

		Element missionQuestionDiv = createMissionQuestionDiv(missionJsonObj, costStructJsonObj);
		doc.body().appendChild(missionQuestionDiv);

		Element agentProposalQuestionDiv = createAgentProposalQuestionDiv(agentPolicyPngFile,
				agentPolicyValuesJsonFile);
		doc.body().appendChild(agentProposalQuestionDiv);

		return doc;
	}

	private Element createMissionQuestionDiv(JSONObject missionJsonObj, JSONObject costStructJsonObj) {
		String mapJsonFilename = (String) missionJsonObj.get("map-file");

		// Mission paragraph and table
		Element missionDiv = createMissionDiv(missionJsonObj, costStructJsonObj);

		// Map image
		String mapPngFilename = FilenameUtils.removeExtension(mapJsonFilename) + ".png";
		Element mapImgDiv = HTMLGeneratorUtils.createImgContainerThirdViewportWidth(mapPngFilename, "Map");

		Element container = HTMLGeneratorUtils.createBlankContainerFullViewportHeight();
		container.appendChild(missionDiv);
		container.appendChild(mapImgDiv);

		return container;
	}

	private Element createMissionDiv(JSONObject missionJsonObj, JSONObject costStructJsonObj) {
		Element container = HTMLGeneratorUtils.createBlankContainer(HTMLGeneratorUtils.W3_TWOTHIRD);

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
		Element table = HTMLGeneratorUtils.createResponsiveBlankTable();
		// Header row
		Element headerRow = table.appendElement("tr");

		// Empty header for quality-attribute unit
		headerRow.appendElement("th");

		// Header for cost
		headerRow.appendElement("th").addClass("w3-right-align").text("Cost ($)");

		for (Object keyObj : costStructJsonObj.keySet()) {
			String qaName = (String) keyObj;
			JSONObject unitCostJsonObj = (JSONObject) costStructJsonObj.get(qaName);
			String descriptiveUnit = (String) unitCostJsonObj.get("unit");
			String formattedCost = (String) unitCostJsonObj.get("cost");

			// Row for each QA unit cost
			Element qaUnitCostRow = table.appendElement("tr");
			qaUnitCostRow.appendElement("td").text(descriptiveUnit);
			qaUnitCostRow.appendElement("td").addClass("w3-right-align").text("$" + formattedCost);
		}

		return table;
	}

	private Element createAgentProposalQuestionDiv(File agentPolicyPngFile, File agentPolicyValuesJsonFile) {
		Element container = HTMLGeneratorUtils.createBlankContainerFullViewportHeight();
		return container;
	}
}
