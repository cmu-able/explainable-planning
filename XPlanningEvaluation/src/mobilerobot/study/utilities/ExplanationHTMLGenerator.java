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

import mobilerobot.utilities.FileIOUtils;

public class ExplanationHTMLGenerator {

	private static final double POLICY_IMG_WIDTH_TO_HEIGHT_RATIO = 0.47;
	private static final int DEFAULT_POLICY_IMG_WIDTH_PX = 500;
	private static final String W3_CONTAINER = "w3-container";
	private static final String W3_CELL = "w3-cell";
	private static final String W3_CENTER = "w3-center";

	private double mPolicyImgWidthToHeightRatio;
	private int mPolicyImgWidthPx;
	private Pattern mJsonFileRefPattern = Pattern.compile("(\\[([^\\[]+)\\.json\\])");

	public ExplanationHTMLGenerator(double policyImgWidthToHeightRatio, int policyImgWidthPx) {
		mPolicyImgWidthToHeightRatio = policyImgWidthToHeightRatio;
		mPolicyImgWidthPx = policyImgWidthPx;
	}

	public void createExplanationHTMLFileBasic(File explanationJsonFile, File outDir) throws IOException, ParseException {
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
		String explanationText = (String) explanationJsonObj.get("Explanation");

		Document doc = createExplanationDocument(explanationText);
		String explanationHTML = doc.toString();

		String explanationHTMLFilename = FilenameUtils.removeExtension(explanationJsonFile.getName()) + ".html";
		Path explanationHTMLPath = outDir.toPath().resolve(explanationHTMLFilename);
		Files.write(explanationHTMLPath, explanationHTML.getBytes());
	}

	private Document createExplanationDocument(String explanationText) {
		Document doc = Jsoup.parse("<html></html>");
		// <link rel="stylesheet" href="https://www.w3schools.com/w3css/4/w3.css">
		doc.appendElement("link").attr("rel", "stylesheet").attr("href", "https://www.w3schools.com/w3css/4/w3.css");

		String[] parts = explanationText.split("\n\n");
		for (int i = 0; i < parts.length; i++) {
			String policyExplanation = parts[i];
			int imgIndex = i + 1;

			Element policySectionDiv = createPolicySectionDiv(policyExplanation, imgIndex);
			doc.appendChild(policySectionDiv);
		}
		return doc;
	}

	private Element createPolicySectionDiv(String policyExplanation, int imgIndex) {
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
			String pngFullFilename = matcher.group(2) + ".png"; // /path/to/policyX.png
			pngFilename = FilenameUtils.getName(pngFullFilename);
			policyExplanationWithImgRef = policyExplanation.replace(jsonFileRef, "(see Figure " + imgIndex + ")");
		}

		Element policyImgDiv = createPolicyImgDiv(pngFilename, widthPx, heightPx, imgIndex);
		Element policyExplanationDiv = createPolicyExplanationDiv(policyExplanationWithImgRef);

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

	private Element createPolicyExplanationDiv(String policyExplanationWithImgRef) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(W3_CELL);

		Element policyExplanationP = new Element("p");
		policyExplanationP.text(policyExplanationWithImgRef);

		container.appendChild(policyExplanationP);
		return container;
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
		ExplanationHTMLGenerator generator = new ExplanationHTMLGenerator(POLICY_IMG_WIDTH_TO_HEIGHT_RATIO,
				DEFAULT_POLICY_IMG_WIDTH_PX);
		generator.createAllExplanationHTMLFiles(rootDir);
	}

}
