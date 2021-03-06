package mobilerobot.study.utilities;

import org.jsoup.nodes.Element;

public class JSTimingUtils {

	private JSTimingUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static Element getJQueryScript() {
		Element jqueryScript = new Element("script");
		jqueryScript.attr("src", "https://ajax.googleapis.com/ajax/libs/jquery/3.4.1/jquery.min.js");
		return jqueryScript;
	}

	public static String getTimingGlobalVarsDeclaration() {
		StringBuilder builder = new StringBuilder();
		builder.append("var start;\n");
		builder.append("var end;\n");
		builder.append("var elapsed;\n");
		return builder.toString();
	}

	public static String getRecordStartTimeWhenDOMReadyLogic() {
		StringBuilder builder = new StringBuilder();
		builder.append("$(document).ready(function() {\n");
		builder.append("\tstart = Date.now();\n");
		builder.append("});");
		return builder.toString();
	}

	public static String getRecordElapsedTimeToLocalStorageFunction(String key) {
		String localStorageSetValueFormat = "localStorage.setItem(\"%s\", %s)";

		StringBuilder builder = new StringBuilder();
		builder.append("function recordElapsedTimeToLocalStorage() {\n");
		builder.append("\tend = Date.now();\n");
		builder.append("\telapsed = end - start;\n");
		builder.append("\tif (typeof(Storage) !== \"undefined\") {\n");
		builder.append("\t\t");
		builder.append(String.format(localStorageSetValueFormat, key, "elapsed"));
		builder.append(";\n");
		builder.append("\t}\n");
		builder.append("}");
		return builder.toString();
	}

	public static String getTimeMeasurementSnippet(String keyPrefix) {
		StringBuilder builder = new StringBuilder();
		builder.append(getTimingGlobalVarsDeclaration());
		builder.append("\n\n");
		builder.append(getRecordStartTimeWhenDOMReadyLogic());
		builder.append("\n\n");
		builder.append(getRecordElapsedTimeToLocalStorageFunction(keyPrefix));
		return builder.toString();
	}
}
