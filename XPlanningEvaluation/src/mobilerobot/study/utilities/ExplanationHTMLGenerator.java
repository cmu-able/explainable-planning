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

	private double mPolicyImgWidthToHeightRatio;
	private int mPolicyImgWidthPx;

	public ExplanationHTMLGenerator(double policyImgWidthToHeightRatio, int policyImgWidthPx) {
		mPolicyImgWidthToHeightRatio = policyImgWidthToHeightRatio;
		mPolicyImgWidthPx = policyImgWidthPx;
	}

	public void createExplanationHTMLFile(File explanationJsonFile, File outDir) throws IOException, ParseException {
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

		Pattern jsonFileRefPattern = Pattern.compile("(\\[([^\\[]+)\\.json\\])");
		Matcher matcher = jsonFileRefPattern.matcher(explanationHTMLStr);
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
