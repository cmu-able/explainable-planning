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

	public static void recordStartTimeToLocalStorage(String activityName) {
		// TODO
	}

	public static void recordEndTimeToLocalStorage(String activityName) {
		// TODO
	}

	public static String getElapsedTimeInMilliSeconds(String activityName) {
		// TODO
		return null;
	}
}
