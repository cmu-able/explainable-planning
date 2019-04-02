package mobilerobot.utilities;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import examples.mobilerobot.dsm.parser.JSONSimpleParserUtils;

public class PolicyJSONParserUtils {

	private PolicyJSONParserUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static String parseStringVar(String varName, JSONObject decisionJsonObj) {
		return parseVar(String.class, varName, decisionJsonObj);
	}

	public static double parseDoubleVar(String varName, JSONObject decisionJsonObj) {
		return JSONSimpleParserUtils.parseDouble(decisionJsonObj, varName);
	}

	public static boolean parseBooleanVar(String varName, JSONObject decisionJsonObj) {
		return parseVar(Boolean.class, varName, decisionJsonObj);
	}

	private static <T> T parseVar(Class<T> varType, String varName, JSONObject decisionJsonObj) {
		JSONObject stateJsonObj = (JSONObject) decisionJsonObj.get("state");
		Object var = stateJsonObj.get(varName);
		return varType.cast(var);
	}

	public static String parseActionType(JSONObject decisionJsonObj) {
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get("action");
		return (String) actionJsonObj.get("type");
	}

	public static String parseStringActionParameter(int index, JSONObject decisionJsonObj) {
		return parseActionParameter(String.class, index, decisionJsonObj);
	}

	public static double parseDoubleActionParameter(int index, JSONObject decisionJsonObj) {
		Object paramObj = parseActionParameter(Object.class, index, decisionJsonObj);
		return JSONSimpleParserUtils.parseDouble(paramObj);
	}

	private static <T> T parseActionParameter(Class<T> paramType, int index, JSONObject decisionJsonObj) {
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get("action");
		JSONArray actionParamJsonArray = (JSONArray) actionJsonObj.get("params");
		Object param = actionParamJsonArray.get(index);
		return paramType.cast(param);
	}
}
