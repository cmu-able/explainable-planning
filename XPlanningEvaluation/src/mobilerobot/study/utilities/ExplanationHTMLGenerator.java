package mobilerobot.study.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import mobilerobot.utilities.FileIOUtils;

public class ExplanationHTMLGenerator {

	private static final String AGENT_POLICY_CAPTION = "Agent's Policy";
	private static final String ALT_POLICY_CAPTION = "Alternative Policy %d";

	private HTMLTableSettings mTableSettings;
	private Pattern mJsonFileRefPattern = Pattern.compile("(\\[(([^\\[]+)\\.json)\\])");

	public ExplanationHTMLGenerator(HTMLTableSettings tableSettings) {
		mTableSettings = tableSettings;
	}

	public void createExplanationHTMLFile(File explanationJsonFile, File outDir) throws IOException, ParseException {
		JSONObject explanationJsonObj = FileIOUtils.readJSONObjectFromFile(explanationJsonFile);
		Document explanationDoc = createExplanationDocument(explanationJsonObj);
		String explanationDocName = FilenameUtils.removeExtension(explanationJsonFile.getName());
		HTMLGeneratorUtils.writeHTMLDocumentToFile(explanationDoc, explanationDocName, outDir);
	}

	public Document createExplanationDocument(JSONObject explanationJsonObj) {
		Document doc = HTMLGeneratorUtils.createHTMLBlankDocument();
		List<Element> policySectionDivs = createExplanationElements(explanationJsonObj, Paths.get("."));
		for (Element policySectionDiv : policySectionDivs) {
			doc.body().appendChild(policySectionDiv);
		}
		return doc;
	}

	public List<Element> createExplanationElements(JSONObject explanationJsonObj, Path explanationPath) {
		String explanationText = (String) explanationJsonObj.get("Explanation");
		// Each paragraph in the explanation text corresponds to a policy
		String[] parts = explanationText.split("\n\n");
		// Solution policy's QA values are to be contrasted with those of each alternative policy
		JSONObject solnPolicyQAValuesJsonObj = null;

		List<Element> policySectionDivs = new ArrayList<>();

		for (int i = 0; i < parts.length; i++) {
			String policyExplanation = parts[i];
			int imgIndex = i;

			Matcher matcher = mJsonFileRefPattern.matcher(policyExplanation);
			String policyJsonFilename = null;
			if (matcher.find()) {
				String policyJsonFullFilename = matcher.group(2); // /path/to/policyX.json
				policyJsonFilename = FilenameUtils.getName(policyJsonFullFilename); // policyX.json
			}

			// QA values of this policy as json object
			JSONObject policyQAValuesJsonObj = (JSONObject) explanationJsonObj.get(policyJsonFilename);

			// First policy is always the solution policy
			if (imgIndex == 0) {
				solnPolicyQAValuesJsonObj = policyQAValuesJsonObj;
			}

			Element policySectionDiv = createPolicySectionDiv(policyExplanation, solnPolicyQAValuesJsonObj,
					policyQAValuesJsonObj, imgIndex, explanationPath);

			policySectionDivs.add(policySectionDiv);
		}

		return policySectionDivs;
	}

	private Element createPolicySectionDiv(String policyExplanation, JSONObject solnPolicyQAValuesJsonObj,
			JSONObject policyQAValuesJsonObj, int imgIndex, Path explanationPath) {
		// Make this container fits the height of the browser
		// Use scroll for overflow content
		Element container = HTMLGeneratorUtils.createBlankRowContainerFullViewportHeight();

		if (Math.floorMod(imgIndex, 2) == 0) {
			container.addClass("w3-pale-yellow");
		} else {
			container.addClass("w3-light-grey");
		}

		String pngFilename = null;
		String policyExplanationWithImgRef = null;

		Matcher matcher = mJsonFileRefPattern.matcher(policyExplanation);
		if (matcher.find()) {
			String jsonFileRef = matcher.group(1); // [/path/to/policyX.json]
			String pngFullFilename = matcher.group(3) + ".png"; // /path/to/policyX.png
			pngFilename = FilenameUtils.getName(pngFullFilename); // policyX.png

			if (imgIndex == 0) {
				// Agent's policy is always at index 0
				policyExplanationWithImgRef = policyExplanation.replace(jsonFileRef, "(see \"Agent's Policy\" figure)");
			} else {
				// Alternative policies start at index 1
				policyExplanationWithImgRef = policyExplanation.replace(jsonFileRef,
						"(see \"Alternative Policy " + imgIndex + "\" figure)");
			}
		}

		Path solnPolicyImgPath = explanationPath.resolve("solnPolicy.png");
		Element solnPolicyImgDiv = createPolicyImgDiv(solnPolicyImgPath, 0);
		Element policyExplanationDiv = createPolicyExplanationDiv(policyExplanationWithImgRef,
				solnPolicyQAValuesJsonObj, policyQAValuesJsonObj, imgIndex);

		container.appendChild(solnPolicyImgDiv);
		container.appendChild(policyExplanationDiv);
		if (imgIndex > 0) {
			Path policyImgPath = explanationPath.resolve(pngFilename);
			Element policyImgDiv = createPolicyImgDiv(policyImgPath, imgIndex);
			addShowLegendButton(policyImgDiv);
			container.appendChild(policyImgDiv);
		} else {
			Element emptyDiv = HTMLGeneratorUtils.createBlankContainer(HTMLGeneratorUtils.W3_THIRD);
			// Add legend button on the right side
			Element showLegendButton = HTMLGeneratorUtils.createShowRightSidebarButton("legend", "☰");
			emptyDiv.appendChild(showLegendButton);
			container.appendChild(emptyDiv);
		}

		return container;
	}

	private Element createPolicyImgDiv(Path policyImgPath, int imgIndex) {
		String policyImgCaption;
		if (imgIndex == 0) {
			// First policy is always the solution policy
			policyImgCaption = AGENT_POLICY_CAPTION;
		} else {
			// Alternative policies start at index 1
			policyImgCaption = String.format(ALT_POLICY_CAPTION, imgIndex);
		}

		return HTMLGeneratorUtils.createResponsiveImgContainer(policyImgPath.toString(), policyImgCaption,
				HTMLGeneratorUtils.W3_THIRD);
	}

	private void addShowLegendButton(Element policyImgDiv) {
		// Make image header inline-block to make room for legend button
		Element policyImgCaption = policyImgDiv.selectFirst("h5");
		policyImgCaption.attr(HTMLGeneratorUtils.CSS_STYLE, "display:inline-block");

		// Add legend button after image header
		Element showLegendButton = HTMLGeneratorUtils.createShowRightSidebarButton("legend", "☰");
		policyImgCaption.after(showLegendButton);
	}

	private Element createPolicyExplanationDiv(String policyExplanationWithImgRef, JSONObject solnPolicyQAValuesJsonObj,
			JSONObject policyQAValuesJsonObj, int imgIndex) {
		Element container = HTMLGeneratorUtils.createBlankContainer(HTMLGeneratorUtils.W3_THIRD);

		// Verbal explanation
		Element policyExplanationP = new Element("p");
		policyExplanationP.text(policyExplanationWithImgRef);

		// Table of QA values
		Element qaValuesTableContainer = mTableSettings.isVerticalTable()
				? createQAValuesTableContainerVertical(solnPolicyQAValuesJsonObj, policyQAValuesJsonObj, imgIndex)
				: createQAValuesTableContainerHorizontal(solnPolicyQAValuesJsonObj, policyQAValuesJsonObj, imgIndex);

		container.appendChild(policyExplanationP);
		container.appendChild(qaValuesTableContainer);
		return container;
	}

	public Element createQAValuesTableContainerVertical(JSONObject agentPolicyQAValuesJsonObj) {
		return createQAValuesTableContainerVertical(agentPolicyQAValuesJsonObj, null, 0);
	}

	private Element createQAValuesTableContainerVertical(JSONObject solnPolicyQAValuesJsonObj,
			JSONObject policyQAValuesJsonObj, int imgIndex) {
		Element tableContainer = HTMLGeneratorUtils.createResponsiveBlankTableContainer();
		Element table = tableContainer.selectFirst("table");
		table.addClass("w3-border");
		table.addClass("w3-bordered");
		table.addClass("w3-centered");

		// Table header: | (empty header for QA column) | Agent's Policy | Alternative Policy |
		Element tableHeaderRow = table.appendElement("tr");
		tableHeaderRow.appendElement("th"); // empty header for QA column
		Element policyHeader = tableHeaderRow.appendElement("th"); // Agent's Policy header
		policyHeader.text(AGENT_POLICY_CAPTION);
		if (imgIndex > 0) {
			Element altPolicyHeader = tableHeaderRow.appendElement("th"); // Alternative Policy header
			altPolicyHeader.text(String.format(ALT_POLICY_CAPTION, imgIndex));
		}

		// Table rows:
		// | [QA 1] | Agent's QA 1 value | Alternative's QA 1 value |
		// | [QA 2] | Agent's QA 2 value | Alternative's QA 2 value |
		// ...
		for (String qaName : mTableSettings.getOrderedQANames()) {
			Element qaTableRow = table.appendElement("tr");

			// QA header text: unit under QA name
			Element qaHeader = qaTableRow.appendElement("th");
			String qaDescriptiveUnit = mTableSettings.getQADescriptiveUnit(qaName);
			qaHeader.appendElement("div").text(qaName);
			qaHeader.appendElement("div").text("(" + qaDescriptiveUnit + ")");

			List<Element> eventTableRows = new ArrayList<>();
			createQAValueCell(solnPolicyQAValuesJsonObj, qaName, qaTableRow, eventTableRows, table);

			if (imgIndex > 0) {
				// Alternative's QA value cell
				createQAValueCell(policyQAValuesJsonObj, qaName, qaTableRow, eventTableRows, table);
			}
		}

		return tableContainer;
	}

	private void createQAValueCell(JSONObject policyQAValuesJsonObj, String qaName, Element qaTableRow,
			List<Element> eventTableRows, Element table) {
		// Policy's QA value cell
		Object policyQAValueObj = policyQAValuesJsonObj.get(qaName);

		if (policyQAValueObj instanceof JSONObject) {
			// Event-based QA value
			JSONObject eventBasedQAValueJsonObj = (JSONObject) policyQAValueObj;

			// Raw QA value cell
			String formattedQAValue = (String) eventBasedQAValueJsonObj.get("Value");
			qaTableRow.appendElement("td").text(formattedQAValue);

			// Additional rows for event-based values
			List<String> orderedEventNames = mTableSettings.getOrderedEventNames(qaName);
			JSONObject eventBasedValuesJsonObj = (JSONObject) eventBasedQAValueJsonObj.get("Event-based Values");

			// If event rows have not been created yet, create and add them to the table
			if (eventTableRows.isEmpty()) {
				for (String eventName : orderedEventNames) {
					// Create a row for each event
					Element eventTableRow = table.appendElement("tr");

					// Each event's descriptive unit (sub-)header
					String eventDescriptiveUnit = mTableSettings.getEventDescriptiveUnit(qaName, eventName);
					eventTableRow.appendElement("td").text(eventDescriptiveUnit);

					eventTableRows.add(eventTableRow);
				}
			}

			// All event rows have been created and added to the table
			for (int i = 0; i < orderedEventNames.size(); i++) {
				Element eventTableRow = eventTableRows.get(i);
				String eventName = orderedEventNames.get(i);

				// Event value cell
				String formattedEventValue = (String) eventBasedValuesJsonObj.get(eventName);
				eventTableRow.appendElement("td").text(formattedEventValue);
			}
		} else {
			// QA value cell
			String formattedQAValue = (String) policyQAValueObj;
			qaTableRow.appendElement("td").text(formattedQAValue);
		}
	}

	public Element createQAValuesTableContainerHorizontal(JSONObject agentPolicyQAValuesJsonObj) {
		return createQAValuesTableContainerHorizontal(null, agentPolicyQAValuesJsonObj, 0);
	}

	private Element createQAValuesTableContainerHorizontal(JSONObject solnPolicyQAValuesJsonObj,
			JSONObject policyQAValuesJsonObj, int imgIndex) {
		Element tableContainer = HTMLGeneratorUtils.createResponsiveBlankTableContainer();
		Element table = tableContainer.selectFirst("table");
		table.addClass("w3-border");
		table.addClass("w3-bordered");
		table.addClass("w3-centered");

		// Table header: [Policy], QA1, QA2, ...
		// Table sub-header: Event1, Event2, ... per event-based QA
		createQAValuesTableHeader(table);

		// Table rows
		// Table row: [Policy ref], QA1 value, QA2 value, ...
		addPolicyQAValuesRow(policyQAValuesJsonObj, imgIndex, table);

		// If this is alternative policy, add a row for the solution policy to contrast to
		if (imgIndex > 0) {
			addPolicyQAValuesRow(solnPolicyQAValuesJsonObj, 0, table);
		}

		return tableContainer;
	}

	private void createQAValuesTableHeader(Element table) {
		// Table header: [Policy], QA1, QA2, ...
		Element tableHeader = new Element("tr");

		// rowspan <- 2 for the sub-header(s) of event-based QA(s)
		String headerRowspan = mTableSettings.hasEventBasedQA() ? "2" : "1";

		// Empty header for Policy column
		tableHeader.appendElement("th").attr("rowspan", headerRowspan);

		// Header for each QA column
		Element qaHeader;
		for (String qaName : mTableSettings.getOrderedQANames()) {
			// Create a header for this QA
			// rowspan is set according to whether there is a sub-header
			qaHeader = tableHeader.appendElement("th").attr("rowspan", headerRowspan);

			if (mTableSettings.isEventBasedQA(qaName)) {
				// This column is for total penalty value of this event-based QA
				qaHeader.appendElement("div").text(qaName);
				qaHeader.appendElement("div").text("(" + qaName + "-penalty" + ")");

				// Additional column for break-down values of this event-based QA
				// Need sub-header for this event-based QA
				// colspan <- # events
				int numEvents = mTableSettings.getOrderedEventNames(qaName).size();
				qaHeader = tableHeader.appendElement("th").attr("colspan", Integer.toString(numEvents));
			}

			// QA header text: unit under QA name
			String qaDescriptiveUnit = mTableSettings.getQADescriptiveUnit(qaName);
			qaHeader.appendElement("div").text(qaName);
			qaHeader.appendElement("div").text("(" + qaDescriptiveUnit + ")");
		}
		table.appendChild(tableHeader);

		// Table sub-header: Event1, Event2, ... per event-based QA
		if (mTableSettings.hasEventBasedQA()) {
			Element tableSubHeader = new Element("tr");

			for (String qaName : mTableSettings.getOrderedQANames()) {
				if (mTableSettings.isEventBasedQA(qaName)) {
					// Columns <- names of events
					for (String eventName : mTableSettings.getOrderedEventNames(qaName)) {
						tableSubHeader.appendElement("th").text(eventName);
					}
				}
			}
			table.appendChild(tableSubHeader);
		}
	}

	private void addPolicyQAValuesRow(JSONObject policyQAValuesJsonObj, int imgIndex, Element table) {
		// Table row: [Policy ref], QA1 value, QA2 value, ...
		Element qaValuesRow = new Element("tr");

		// Highlight the row for solution policy
		if (imgIndex == 0) {
			qaValuesRow.addClass("w3-pale-red");
			qaValuesRow.appendElement("td").text(AGENT_POLICY_CAPTION);
		} else {
			qaValuesRow.appendElement("td").text(String.format(ALT_POLICY_CAPTION, imgIndex));
		}

		for (String qaName : mTableSettings.getOrderedQANames()) {
			Object qaValueObj = policyQAValuesJsonObj.get(qaName);

			if (qaValueObj instanceof JSONObject) {
				// Event-based QA value
				JSONObject eventBasedQAValueJsonObj = (JSONObject) qaValueObj;

				// Raw QA value
				String formattedQAValue = (String) eventBasedQAValueJsonObj.get("Value");
				qaValuesRow.appendElement("td").text(formattedQAValue);

				// Event-based values
				JSONObject eventBasedValuesJsonObj = (JSONObject) eventBasedQAValueJsonObj.get("Event-based Values");
				for (String eventName : mTableSettings.getOrderedEventNames(qaName)) {
					String formattedEventValue = (String) eventBasedValuesJsonObj.get(eventName);
					qaValuesRow.appendElement("td").text(formattedEventValue);
				}
			} else {
				String formattedQAValue = (String) qaValueObj;
				qaValuesRow.appendElement("td").text(formattedQAValue);
			}
		}
		table.appendChild(qaValuesRow);
	}

	public void createAllExplanationHTMLFiles(File rootDir) throws IOException, ParseException {
		File[] explanationJsonFiles = FileIOUtils.listFilesWithContainFilter(rootDir, "explanation", ".json");
		for (File explanationJsonFile : explanationJsonFiles) {
			createExplanationHTMLFile(explanationJsonFile, rootDir);
		}

		FileFilter dirFilter = File::isDirectory;
		for (File subDir : rootDir.listFiles(dirFilter)) {
			createAllExplanationHTMLFiles(subDir);
		}
	}

	public static void main(String[] args) throws IOException, ParseException {
		String pathname = args[0];
		File rootDir = new File(pathname);

		HTMLTableSettings tableSettings = getMobileRobotHTMLTableSettings();
		ExplanationHTMLGenerator generator = new ExplanationHTMLGenerator(tableSettings);
		generator.createAllExplanationHTMLFiles(rootDir);
	}

	public static HTMLTableSettings getMobileRobotHTMLTableSettings() {
		HTMLTableSettings tableSettings = new HTMLTableSettings(true);
		tableSettings.appendQAName(TravelTimeQFunction.NAME);
		tableSettings.appendQAName(CollisionEvent.NAME);
		tableSettings.appendQAName(IntrusiveMoveEvent.NAME);
		tableSettings.appendEventName(IntrusiveMoveEvent.NAME, "non-intrusive");
		tableSettings.appendEventName(IntrusiveMoveEvent.NAME, "somewhat-intrusive");
		tableSettings.appendEventName(IntrusiveMoveEvent.NAME, "very-intrusive");
		tableSettings.putQADescriptiveUnit(TravelTimeQFunction.NAME, "minutes");
		tableSettings.putQADescriptiveUnit(CollisionEvent.NAME, "expected #");
		tableSettings.putQADescriptiveUnit(IntrusiveMoveEvent.NAME, "penalty");
		tableSettings.putEventDescriptiveUnit(IntrusiveMoveEvent.NAME, "# " + IntrusiveMoveEvent.NAME + " locations");
		return tableSettings;
	}

}
