package mobilerobot.utilities;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import uiconnector.JSONSimpleParserUtils;

public class PolicyJSONParserUtils {

	private static final String STATE_KEY = "state";
	private static final String ACTION_KEY = "action";

	private PolicyJSONParserUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static boolean containsVar(String varName, JSONObject decisionJsonObj) {
		JSONObject stateJsonObj = (JSONObject) decisionJsonObj.get(STATE_KEY);
		return stateJsonObj.containsKey(varName);
	}

	public static String parseStringVar(String varName, JSONObject decisionJsonObj) {
		return parseVar(String.class, varName, decisionJsonObj);
	}

	public static double parseDoubleVar(String varName, JSONObject decisionJsonObj) {
		JSONObject stateJsonObj = (JSONObject) decisionJsonObj.get(STATE_KEY);
		return JSONSimpleParserUtils.parseDouble(stateJsonObj, varName);
	}

	public static boolean parseBooleanVar(String varName, JSONObject decisionJsonObj) {
		return parseVar(Boolean.class, varName, decisionJsonObj);
	}

	private static <T> T parseVar(Class<T> varType, String varName, JSONObject decisionJsonObj) {
		JSONObject stateJsonObj = (JSONObject) decisionJsonObj.get(STATE_KEY);
		Object var = stateJsonObj.get(varName);
		return varType.cast(var);
	}

	public static String parseActionType(JSONObject decisionJsonObj) {
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get(ACTION_KEY);
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
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get(ACTION_KEY);
		JSONArray actionParamJsonArray = (JSONArray) actionJsonObj.get("params");
		Object param = actionParamJsonArray.get(index);
		return paramType.cast(param);
	}
}
