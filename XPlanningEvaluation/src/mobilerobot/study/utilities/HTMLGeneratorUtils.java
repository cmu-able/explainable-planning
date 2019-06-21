package mobilerobot.study.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HTMLGeneratorUtils {

	private static final String W3_CONTAINER = "w3-container";

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
}
