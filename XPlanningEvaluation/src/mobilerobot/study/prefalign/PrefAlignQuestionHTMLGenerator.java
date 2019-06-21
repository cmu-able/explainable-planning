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

	public PrefAlignQuestionHTMLGenerator() {

	}

	public void createPrefAlignQuestionHTMLFile(File missionJsonFile, int agentIndex, File questionDir)
			throws IOException, ParseException {
		JSONObject missionJsonObj = FileIOUtils.readJSONObjectFromFile(missionJsonFile);
		Document questionDoc = createPrefAlignQuestionDocument(missionJsonObj, agentIndex);
		String questionDocName = null;
		HTMLGeneratorUtils.writeHTMLDocumentToFile(questionDoc, questionDocName, questionDir);
	}

	private Document createPrefAlignQuestionDocument(JSONObject missionJsonObj, int agentIndex) {
		Document doc = HTMLGeneratorUtils.createHTMLBlankDocument();

		Element missionQuestionDiv = createMissionQuestionDiv(missionJsonObj);
		doc.body().appendChild(missionQuestionDiv);

		Element agentProposalQuestionDiv = createAgentProposalQuestionDiv();
		doc.body().appendChild(agentProposalQuestionDiv);

		return doc;
	}

	private Element createMissionQuestionDiv(JSONObject missionJsonObj) {
		String mapJsonFilename = (String) missionJsonObj.get("map-file");

		// Mission paragraph and table
		Element missionDiv = createMissionDiv(missionJsonObj);

		// Map image
		String mapPngFilename = FilenameUtils.removeExtension(mapJsonFilename) + ".png";
		Element mapImgDiv = HTMLGeneratorUtils.createImgContainerThirdViewportWidth(mapPngFilename, "Map");

		Element container = HTMLGeneratorUtils.createBlankContainerFullViewportHeight();
		container.appendChild(missionDiv);
		container.appendChild(mapImgDiv);

		return container;
	}

	private Element createMissionDiv(JSONObject missionJsonObj) {
		String startID = (String) missionJsonObj.get("start-id");
		String goalID = (String) missionJsonObj.get("goal-id");

		// Mission paragraph
		Element missionP = new Element("p");
		missionP.text("");

		// Cost-structure table
		// TODO

		return null;
	}

	private Element createAgentProposalQuestionDiv() {
		Element container = HTMLGeneratorUtils.createBlankContainerFullViewportHeight();
		return container;
	}
}
