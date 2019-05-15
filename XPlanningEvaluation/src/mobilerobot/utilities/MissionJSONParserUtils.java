package mobilerobot.utilities;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import examples.mobilerobot.dsm.parser.JSONSimpleParserUtils;

public class MissionJSONParserUtils {

	private MissionJSONParserUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static Map<String, Double> parseScalingConsts(JSONObject missionJsonObj) {
		Map<String, Double> scalingConsts = new HashMap<>();
		JSONArray prefJsonArray = (JSONArray) missionJsonObj.get("preference-info");
		for (Object obj : prefJsonArray) {
			JSONObject prefJsonObj = (JSONObject) obj;
			String objectiveName = (String) prefJsonObj.get("objective");
			Double scalingConst = JSONSimpleParserUtils.parseDouble(prefJsonObj, "scaling-const");
			scalingConsts.put(objectiveName, scalingConst);
		}
		return scalingConsts;
	}
}
