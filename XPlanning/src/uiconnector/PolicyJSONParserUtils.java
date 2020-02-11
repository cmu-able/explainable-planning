package uiconnector;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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

	public static int parseIntVar(String varName, JSONObject decisionJsonObj) {
		// JSONSimple uses "long" type for whole numbers
		Long longVar = parseVar(Long.class, varName, decisionJsonObj);
		return longVar.intValue();
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

	public static int parseIntActionParameter(int index, JSONObject decisionJsonObj) {
		// JSONSimple uses "long" type for whole numbers
		Long longActionParam = parseActionParameter(Long.class, index, decisionJsonObj);
		return longActionParam.intValue();
	}

	public static boolean parseBooleanActionParameter(int index, JSONObject decisionJsonObj) {
		return parseActionParameter(Boolean.class, index, decisionJsonObj);
	}

	private static <T> T parseActionParameter(Class<T> paramType, int index, JSONObject decisionJsonObj) {
		Object param = getActionParameterObject(index, decisionJsonObj);
		return paramType.cast(param);
	}

	public static int getNumActionParameters(JSONObject decisionJsonObj) {
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get(ACTION_KEY);
		JSONArray actionParamJsonArray = (JSONArray) actionJsonObj.get("params");
		return actionParamJsonArray.size();
	}

	public static Class<?> getActionParameterType(int index, JSONObject decisionJsonObj) {
		Object param = getActionParameterObject(index, decisionJsonObj);
		return param.getClass();
	}

	private static Object getActionParameterObject(int index, JSONObject decisionJsonObj) {
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get(ACTION_KEY);
		JSONArray actionParamJsonArray = (JSONArray) actionJsonObj.get("params");
		return actionParamJsonArray.get(index);
	}
}
