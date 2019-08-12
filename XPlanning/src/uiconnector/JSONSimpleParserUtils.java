package uiconnector;

import org.json.simple.JSONObject;

public class JSONSimpleParserUtils {

	private JSONSimpleParserUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static double parseDouble(JSONObject jsonObject, String key) {
		Object obj = jsonObject.get(key);
		return parseDouble(obj);
	}

	public static int parseInt(JSONObject jsonObject, String key) {
		// JSONSimple uses "long" type for whole numbers
		Long valueLong = (Long) jsonObject.get(key);
		return valueLong.intValue();
	}

	public static double parseDouble(Object obj) {
		if (obj instanceof Long) {
			// JSONSimple uses "long" type for whole numbers
			Long valueLong = (Long) obj;
			return valueLong.doubleValue();
		}
		return (double) obj;
	}
}
