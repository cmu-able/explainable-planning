package mobilerobot.study.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import mobilerobot.utilities.FileIOUtils;

public class ExplanationHTMLGenerator {

	private static final double POLICY_IMG_WIDTH_TO_HEIGHT_RATIO = 0.47;
	private static final int DEFAULT_POLICY_IMG_WIDTH_PX = 500;
	private static final String W3_CONTAINER = "w3-container";
	private static final String W3_CELL = "w3-cell";
	private static final String W3_CENTER = "w3-center";

	private double mPolicyImgWidthToHeightRatio;
	private int mPolicyImgWidthPx;
	private HTMLTableSettings mTableSettings;
	private Pattern mJsonFileRefPattern = Pattern.compile("(\\[(([^\\[]+)\\.json)\\])");

	public ExplanationHTMLGenerator(double policyImgWidthToHeightRatio, int policyImgWidthPx,
			HTMLTableSettings tableSettings) {
		mPolicyImgWidthToHeightRatio = policyImgWidthToHeightRatio;
		mPolicyImgWidthPx = policyImgWidthPx;
		mTableSettings = tableSettings;
	}

	public void createExplanationHTMLFileBasic(File explanationJsonFile, File outDir)
			throws IOException, ParseException {
		JSONObject explanationJsonObj = FileIOUtils.readJSONObjectFromFile(explanationJsonFile);
		String explanationText = (String) explanationJsonObj.get("Explanation");

		Document doc = Jsoup.parse("<html></html>");
		doc.body().appendElement("div");
		Element div = doc.selectFirst("div");
		div.text(explanationText);
		String explanationHTMLStr = doc.toString();

		String explanationHTMLWithImages = explanationHTMLStr;
		int widthPx = mPolicyImgWidthPx;
		int heightPx = (int) Math.round(widthPx / mPolicyImgWidthToHeightRatio);
		String imgHTMLElementStr = "<img src=\"%s\" width=\"%d\" height=\"%d\">";

		Matcher matcher = mJsonFileRefPattern.matcher(explanationHTMLStr);
		while (matcher.find()) {
			String jsonFileRef = matcher.group(1); // [/path/to/policyX.json]
			String pngFullFilename = matcher.group(2) + ".png"; // /path/to/policyX.png
			String pngFilename = FilenameUtils.getName(pngFullFilename);
			String imgHTMLElement = String.format(imgHTMLElementStr, pngFilename, widthPx, heightPx);
			explanationHTMLWithImages = explanationHTMLWithImages.replace(jsonFileRef, imgHTMLElement);
		}

		String explanationHTMLFilename = FilenameUtils.removeExtension(explanationJsonFile.getName()) + ".html";
		Path explanationHTMLPath = outDir.toPath().resolve(explanationHTMLFilename);
		Files.write(explanationHTMLPath, explanationHTMLWithImages.getBytes());
	}

	public void createExplanationHTMLFile(File explanationJsonFile, File outDir) throws IOException, ParseException {
		JSONObject explanationJsonObj = FileIOUtils.readJSONObjectFromFile(explanationJsonFile);
		Document doc = createExplanationDocument(explanationJsonObj);
		String explanationHTML = doc.toString();

		String explanationHTMLFilename = FilenameUtils.removeExtension(explanationJsonFile.getName()) + ".html";
		Path explanationHTMLPath = outDir.toPath().resolve(explanationHTMLFilename);
		Files.write(explanationHTMLPath, explanationHTML.getBytes());
	}

	private Document createExplanationDocument(JSONObject explanationJsonObj) {
		Document doc = Jsoup.parse("<html></html>");
		// <link rel="stylesheet" href="https://www.w3schools.com/w3css/4/w3.css">
		doc.appendElement("link").attr("rel", "stylesheet").attr("href", "https://www.w3schools.com/w3css/4/w3.css");

		String explanationText = (String) explanationJsonObj.get("Explanation");
		// Each paragraph in the explanation text corresponds to a policy
		String[] parts = explanationText.split("\n\n");
		// Solution policy's QA values are to be contrasted with those of each alternative policy
		JSONObject solnPolicyQAValuesJsonObj = null;

		for (int i = 0; i < parts.length; i++) {
			String policyExplanation = parts[i];
			int imgIndex = i + 1;

			Matcher matcher = mJsonFileRefPattern.matcher(policyExplanation);
			String policyJsonFilename = null;
			if (matcher.find()) {
				String policyJsonFullFilename = matcher.group(2); // /path/to/policyX.json
				policyJsonFilename = FilenameUtils.getName(policyJsonFullFilename); // policyX.json
			}

			// QA values of this policy as json object
			JSONObject policyQAValuesJsonObj = (JSONObject) explanationJsonObj.get(policyJsonFilename);

			// First policy is always the solution policy
			if (imgIndex == 1) {
				solnPolicyQAValuesJsonObj = policyQAValuesJsonObj;
			}

			Element policySectionDiv = createPolicySectionDiv(policyExplanation, policyQAValuesJsonObj,
					solnPolicyQAValuesJsonObj, imgIndex);
			doc.appendChild(policySectionDiv);
		}
		return doc;
	}

	private Element createPolicySectionDiv(String policyExplanation, JSONObject policyQAValuesJsonObj,
			JSONObject solnPolicyQAValuesJsonObj, int imgIndex) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		if (Math.floorMod(imgIndex, 2) == 0) {
			container.addClass("w3-pale-yellow");
		} else {
			container.addClass("w3-light-grey");
		}

		int widthPx = mPolicyImgWidthPx;
		int heightPx = (int) Math.round(widthPx / mPolicyImgWidthToHeightRatio);
		String pngFilename = null;
		String policyExplanationWithImgRef = null;

		Matcher matcher = mJsonFileRefPattern.matcher(policyExplanation);
		if (matcher.find()) {
			String jsonFileRef = matcher.group(1); // [/path/to/policyX.json]
			String pngFullFilename = matcher.group(3) + ".png"; // /path/to/policyX.png
			pngFilename = FilenameUtils.getName(pngFullFilename); // policyX.png
			policyExplanationWithImgRef = policyExplanation.replace(jsonFileRef, "(see Figure " + imgIndex + ")");
		}

		Element solnPolicyImgDiv = createPolicyImgDiv("solnPolicy.png", widthPx, heightPx, 1);
		Element policyExplanationDiv = createPolicyExplanationDiv(policyExplanationWithImgRef, policyQAValuesJsonObj,
				solnPolicyQAValuesJsonObj, imgIndex);

		container.appendChild(solnPolicyImgDiv);
		container.appendChild(policyExplanationDiv);
		if (imgIndex > 1) {
			Element policyImgDiv = createPolicyImgDiv(pngFilename, widthPx, heightPx, imgIndex);
			container.appendChild(policyImgDiv);
		}

		return container;
	}

	private Element createPolicyImgDiv(String pngFilename, int widthPx, int heightPx, int imgIndex) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_CELL);
		container.addClass(W3_CENTER);

		Element policyImg = new Element("img");
		policyImg.attr("src", pngFilename);
		policyImg.attr("width", Integer.toString(widthPx));
		policyImg.attr("height", Integer.toString(heightPx));

		Element policyImgCaption = new Element("h5");
		policyImgCaption.text("Figure " + imgIndex);

		container.appendChild(policyImg);
		container.appendChild(policyImgCaption);
		return container;
	}

	private Element createPolicyExplanationDiv(String policyExplanationWithImgRef, JSONObject policyQAValuesJsonObj,
			JSONObject solnPolicyQAValuesJsonObj, int imgIndex) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_CELL);

		// Verbal explanation
		Element policyExplanationP = new Element("p");
		policyExplanationP.text(policyExplanationWithImgRef);

		// Table of QA values
		Element qaValuesTable = createQAValuesTableVertical(solnPolicyQAValuesJsonObj, policyQAValuesJsonObj, imgIndex);

		container.appendChild(policyExplanationP);
		container.appendChild(qaValuesTable);
		return container;
	}

	private Element createQAValuesTableVertical(JSONObject solnPolicyQAValuesJsonObj, JSONObject policyQAValuesJsonObj,
			int imgIndex) {
		Element table = new Element("table");
		table.addClass("w3-table");
		table.addClass("w3-border");
		table.addClass("w3-bordered");
		table.addClass("w3-centered");

		// Table header: | (empty header for QA column) | Agent's Policy | Alternative Policy |
		Element tableHeaderRow = table.appendElement("tr");
		tableHeaderRow.appendElement("th"); // empty header for QA column
		Element policyHeader = tableHeaderRow.appendElement("th"); // Agent's Policy header
		policyHeader.appendElement("div").text("Agent's Policy");
		policyHeader.appendElement("div").text("(Figure 1)");
		if (imgIndex > 1) {
			Element altPolicyHeader = tableHeaderRow.appendElement("th"); // Alternative Policy header
			altPolicyHeader.appendElement("div").text("Alternative Policy");
			altPolicyHeader.appendElement("div").text("(Figure " + imgIndex + ")");
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

			if (imgIndex > 1) {
				// Alternative's QA value cell
				createQAValueCell(policyQAValuesJsonObj, qaName, qaTableRow, eventTableRows, table);
			}
		}

		return table;
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

					// Event name header
					eventTableRow.appendElement("th").text(eventName);
					// TODO: unit

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

	private Element createQAValuesTableHorizontal(JSONObject policyQAValuesJsonObj,
			JSONObject solnPolicyQAValuesJsonObj, int imgIndex) {
		Element table = new Element("table");
		table.addClass("w3-table");
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
		if (imgIndex > 1) {
			addPolicyQAValuesRow(solnPolicyQAValuesJsonObj, 1, table);
		}

		return table;
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
		if (imgIndex == 1) {
			qaValuesRow.addClass("w3-pale-red");
		}

		qaValuesRow.appendElement("td").text("Figure " + imgIndex);

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
		FilenameFilter explanationJsonFileFilter = (dir, name) -> name.toLowerCase().contains("explanation")
				&& name.toLowerCase().endsWith(".json");
		for (File explanationJsonFile : rootDir.listFiles(explanationJsonFileFilter)) {
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

		HTMLTableSettings tableSettings = new HTMLTableSettings();
		tableSettings.appendQAName(TravelTimeQFunction.NAME);
		tableSettings.appendQAName(CollisionEvent.NAME);
		tableSettings.appendQAName(IntrusiveMoveEvent.NAME);
		tableSettings.appendEventName(IntrusiveMoveEvent.NAME, "non-intrusive");
		tableSettings.appendEventName(IntrusiveMoveEvent.NAME, "somewhat-intrusive");
		tableSettings.appendEventName(IntrusiveMoveEvent.NAME, "very-intrusive");
		tableSettings.putQADescriptiveUnit(TravelTimeQFunction.NAME, "minutes");
		tableSettings.putQADescriptiveUnit(CollisionEvent.NAME, "number of collisions");
		tableSettings.putQADescriptiveUnit(IntrusiveMoveEvent.NAME, "number of visited locations");

		ExplanationHTMLGenerator generator = new ExplanationHTMLGenerator(POLICY_IMG_WIDTH_TO_HEIGHT_RATIO,
				DEFAULT_POLICY_IMG_WIDTH_PX, tableSettings);
		generator.createAllExplanationHTMLFiles(rootDir);
	}

}
