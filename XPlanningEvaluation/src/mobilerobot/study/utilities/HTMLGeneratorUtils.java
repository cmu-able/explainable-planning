package mobilerobot.study.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HTMLGeneratorUtils {

	// w3 container sizes
	public static final String W3_THIRD = "w3-third";
	public static final String W3_TWOTHIRD = "w3-twothird";

	private static final String W3_CONTAINER = "w3-container";
	private static final String W3_CENTER = "w3-center";

	private HTMLGeneratorUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static Document createHTMLBlankDocument() {
		Document doc = Jsoup.parse("<html></html>");
		// <link rel="stylesheet" href="https://www.w3schools.com/w3css/4/w3.css">
		Element link = new Element("link");
		link.attr("rel", "stylesheet").attr("href", "https://www.w3schools.com/w3css/4/w3.css");
		doc.body().before(link);
		return doc;
	}

	public static void writeHTMLDocumentToFile(Document document, String documentName, File outDir) throws IOException {
		String explanationHTML = document.toString();
		String explanationHTMLFilename = documentName + ".html";
		Path explanationHTMLPath = outDir.toPath().resolve(explanationHTMLFilename);
		Files.write(explanationHTMLPath, explanationHTML.getBytes());
	}

	public static Element createBlankContainerFullViewportHeight() {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);

		// Make this container fits the height of the browser
		// Use scroll for overflow content
		container.attr("style", "height:100vh;overflow:scroll");

		return container;
	}

	public static Element createBlankContainer(String w3SizeClass) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		container.addClass(w3SizeClass);
		return container;
	}

	public static Element createImgContainerThirdViewportWidth(String imgFilename, String imgCaption) {
		Element container = createBlankContainer(W3_THIRD);
		container.addClass(W3_CENTER);

		Element imgCaptionHeader = new Element("h5");
		imgCaptionHeader.text(imgCaption);

		Element img = new Element("img");
		img.addClass("w3-image"); // automatically resize image to fit its container
		img.addClass("w3-card");
		img.attr("src", imgFilename);

		// Make this image fits the height of the container with room for image caption
		img.attr("style", "height:90%");

		container.appendChild(imgCaptionHeader);
		container.appendChild(img);
		return container;
	}

	public static Element createResponsiveBlankTable() {
		Element table = new Element("table");
		table.addClass("w3-table");
		table.addClass("w3-responsive");
		return table;
	}
}