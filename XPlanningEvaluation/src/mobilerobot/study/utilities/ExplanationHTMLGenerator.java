package mobilerobot.study.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
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
		String[] parts = explanationText.split("\n\n");
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

			Element policySectionDiv = createPolicySectionDiv(policyExplanation, policyQAValuesJsonObj, imgIndex);
			doc.appendChild(policySectionDiv);
		}
		return doc;
	}

	private Element createPolicySectionDiv(String policyExplanation, JSONObject policyQAValuesJsonObj, int imgIndex) {
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
			pngFilename = FilenameUtils.getName(pngFullFilename);
			policyExplanationWithImgRef = policyExplanation.replace(jsonFileRef, "(see Figure " + imgIndex + ")");
		}

		Element policyImgDiv = createPolicyImgDiv(pngFilename, widthPx, heightPx, imgIndex);
		Element policyExplanationDiv = createPolicyExplanationDiv(policyExplanationWithImgRef, policyQAValuesJsonObj,
				imgIndex);

		container.appendChild(policyImgDiv);
		container.appendChild(policyExplanationDiv);
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
			int imgIndex) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_CELL);

		// Verbal explanation
		Element policyExplanationP = new Element("p");
		policyExplanationP.text(policyExplanationWithImgRef);

		// Table of QA values
		Element qaValuesTable = createQAValuesTable(policyQAValuesJsonObj, imgIndex);

		container.appendChild(policyExplanationP);
		container.appendChild(qaValuesTable);
		return container;
	}

	private Element createQAValuesTable(JSONObject policyQAValuesJsonObj, int imgIndex) {
		Element table = new Element("table");
		table.addClass("w3-table");

		// Table header: [Policy], QA1, QA2, ...
		// Table sub-header: Event1, Event2, ... per event-based QA
		createQAValuesTableHeader(table);

		// Table rows
		// Table row: [Policy ref], QA1 value, QA2 value, ...
		Element qaValuesRow = new Element("tr");
		qaValuesRow.appendElement("td").text("Figure " + imgIndex);

		for (String qaName : mTableSettings.getOrderedQANames()) {
			Object qaValueObj = policyQAValuesJsonObj.get(qaName);

			if (qaValueObj instanceof JSONObject) {
				// Event-based QA value
				JSONObject eventBasedQAValue = (JSONObject) qaValueObj;
				for (String eventName : mTableSettings.getOrderedEventNames(qaName)) {
					String formattedEventValue = (String) eventBasedQAValue.get(eventName);
					qaValuesRow.appendElement("td").text(formattedEventValue);
				}
			} else {
				String formattedQAValue = (String) qaValueObj;
				qaValuesRow.appendElement("td").text(formattedQAValue);
			}
		}
		table.appendChild(qaValuesRow);
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
		for (String qaName : mTableSettings.getOrderedQANames()) {
			if (mTableSettings.isEventBasedQA(qaName)) {
				// Need sub-header for this event-based QA
				// colspan <- # events
				int numEvents = mTableSettings.getOrderedEventNames(qaName).size();
				tableHeader.appendElement("th").attr("colspan", Integer.toString(numEvents)).text(qaName);
			} else {
				// No sub-header for this QA
				// rowspan is set according to whether there is a sub-header
				tableHeader.appendElement("th").attr("rowspan", headerRowspan).text(qaName);
			}
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

		ExplanationHTMLGenerator generator = new ExplanationHTMLGenerator(POLICY_IMG_WIDTH_TO_HEIGHT_RATIO,
				DEFAULT_POLICY_IMG_WIDTH_PX, tableSettings);
		generator.createAllExplanationHTMLFiles(rootDir);
	}

}
