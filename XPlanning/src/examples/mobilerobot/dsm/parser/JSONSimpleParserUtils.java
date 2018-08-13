package examples.mobilerobot.dsm.parser;

import org.json.simple.JSONObject;

public class JSONSimpleParserUtils {

	private JSONSimpleParserUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static double parseDouble(JSONObject jsonObject, String key) {
		// JSONSimple only uses "long" type for numbers
		Long valueLong = (Long) jsonObject.get(key);
		return valueLong.doubleValue();
	}

	public static int parseInt(JSONObject jsonObject, String key) {
		// JSONSimple only uses "long" type for numbers
		Long valueLong = (Long) jsonObject.get(key);
		return valueLong.intValue();
	}
}
