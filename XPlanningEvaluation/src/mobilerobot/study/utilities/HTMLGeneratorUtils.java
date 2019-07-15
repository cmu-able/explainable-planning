package mobilerobot.study.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class HTMLGeneratorUtils {

	public static final String CSS_STYLE = "style";

	// w3 container sizes
	public static final String W3_THIRD = "w3-third";
	public static final String W3_HALF = "w3-half";
	public static final String W3_TWOTHIRD = "w3-twothird";

	private static final String W3_CONTAINER = "w3-container";
	private static final String W3_ROW = "w3-row"; // container of responsive inner-containers
	private static final String W3_CENTER = "w3-center";

	private HTMLGeneratorUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static Document createHTMLBlankDocument() {
		Document doc = Jsoup.parse("<!DOCTYPE html><html lang=\"en\"></html>");
		doc.head().appendElement("title").text("Untitled");
		// <link rel="stylesheet" href="https://www.w3schools.com/w3css/4/w3.css">
		Element link = new Element("link");
		link.attr("rel", "stylesheet").attr("href", "https://www.w3schools.com/w3css/4/w3.css");
		doc.head().appendChild(link);
		return doc;
	}

	public static void writeHTMLDocumentToFile(Document document, String documentName, File outDir) throws IOException {
		String explanationHTML = document.toString();
		String explanationHTMLFilename = documentName + ".html";
		Path explanationHTMLPath = outDir.toPath().resolve(explanationHTMLFilename);
		Files.write(explanationHTMLPath, explanationHTML.getBytes());
	}

	public static Element createBlankRowContainerFullViewportHeight() {
		Element container = new Element("div");
		// w3-row is a container class for responsive inner-containers
		container.addClass(W3_ROW);

		// Make this container fits the height of the browser
		// Use scroll for overflow content
		container.attr(CSS_STYLE, "height:100vh;overflow:auto");

		return container;
	}

	public static Element createBlankRowContainer(String w3SizeClass) {
		Element container = new Element("div");
		// w3-row is a container class for responsive inner-containers
		container.addClass(W3_ROW);
		container.addClass(w3SizeClass);
		return container;
	}

	public static Element createBlankContainer() {
		return createBlankContainer(null);
	}

	public static Element createBlankContainer(String w3SizeClass) {
		Element container = new Element("div");
		container.addClass(W3_CONTAINER);
		if (w3SizeClass != null) {
			container.addClass(w3SizeClass);
		}
		return container;
	}

	public static Element createResponsiveImg(String imgPath, String imgCaption) {
		Element img = new Element("img");
		img.addClass("w3-image"); // automatically resize image to fit its container
		img.attr("src", imgPath);
		img.attr("alt", imgCaption);
		return img;
	}

	public static Element createResponsiveImgContainer(String imgPath, String imgCaption, String w3SizeClass) {
		Element container = createBlankContainer(w3SizeClass);
		container.addClass(W3_CENTER);
		// Vertical image must always be fully displayed within a browser-window height
		container.attr(CSS_STYLE, "height:100vh");

		Element imgCaptionHeader = new Element("h5");
		imgCaptionHeader.text(imgCaption);

		Element img = createResponsiveImg(imgPath, imgCaption);
		img.addClass("w3-card");

		// Make this image fits the height of the container with room for image caption
		img.attr(CSS_STYLE, "height:90%");

		container.appendChild(imgCaptionHeader);
		container.appendChild(img);
		return container;
	}

	public static Element createResponsiveBlankTableContainer() {
		Element container = new Element("div");
		container.addClass("w3-responsive");
		// Can't apply w3-responsive class to table directly, because it'd leave a gap in table to fit outer container
		Element table = new Element("table");
		table.addClass("w3-table");
		container.appendChild(table);
		return container;
	}

	public static Element createInstructionContainer(String instruction) {
		Element container = createBlankContainer();
		container.addClass("w3-margin");

		Element instructionHeader = new Element("h4");
		instructionHeader.appendElement("i").text(instruction);
		container.appendChild(instructionHeader);
		return container;
	}

	public static Element createBlankRightSidebar(String sidebarId, double widthPercent) {
		Element sidebar = new Element("div");
		sidebar.addClass("w3-sidebar");
		sidebar.addClass("w3-bar-block");
		sidebar.attr("id", sidebarId);
		String style = String.format("width:%.1f%%;display:none;right:0", widthPercent);
		sidebar.attr(CSS_STYLE, style);

		Element closeButton = new Element("button");
		closeButton.addClass("w3-bar-item");
		closeButton.addClass("w3-button");
		closeButton.addClass("w3-large");
		closeButton.attr("onclick", "close_" + sidebarId + "()");
		closeButton.html("Close &times;");

		sidebar.appendChild(closeButton);
		return sidebar;
	}

	public static Element createShowRightSidebarButton(String sidebarId, String label) {
		Element openButton = new Element("button");
		openButton.addClass("w3-button");
		openButton.addClass("w3-large");
		openButton.addClass("w3-teal");
		openButton.addClass("w3-right");
		openButton.attr("onclick", "open_" + sidebarId + "()");
		openButton.text(label);
		return openButton;
	}

	public static Element createOpenCloseSidebarScript(String sidebarId) {
		String openFunction = getSidebarDisplayFunction(sidebarId, "open", "block");
		String closeFunction = getSidebarDisplayFunction(sidebarId, "close", "none");

		Element script = new Element("script");
		script.appendText(openFunction);
		script.appendText(closeFunction);
		return script;
	}

	private static String getSidebarDisplayFunction(String sidebarId, String action, String display) {
		StringBuilder builder = new StringBuilder();
		builder.append("function ");
		builder.append(action);
		builder.append("_");
		builder.append(sidebarId);
		builder.append("() { ");
		builder.append("document.getElementById(\"");
		builder.append(sidebarId);
		builder.append("\").style.display = \"");
		builder.append(display);
		builder.append("\"; }");
		return builder.toString();
	}
}
