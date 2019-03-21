package mobilerobot.missiongen;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.MobileRobotXMDPBuilder;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.models.Occlusion;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.utilities.MapTopologyUtils;

public class MissionJSONGenerator {

	private static final String START_NODE_ID = "L1";
	private static final String GOAL_NODE_ID = "L32";

	public static JSONObject createMissionJsonObject(String mapFilename, Map<String, Double> scalingConsts)
			throws URISyntaxException, MapTopologyException, IOException, ParseException {
		JSONObject missionJsonObj = new JSONObject();
		missionJsonObj.put("start-id", START_NODE_ID);
		missionJsonObj.put("goal-id", GOAL_NODE_ID);
		missionJsonObj.put("map-file", mapFilename);

		JSONObject timePrefJsonObj = new JSONObject();
		timePrefJsonObj.put("objective", "travelTime");
		timePrefJsonObj.put("min-step-value", 0);
		timePrefJsonObj.put("max-step-value", getMaxStepTravelTime(mapFilename));
		timePrefJsonObj.put("scaling-const", scalingConsts.get("travelTime"));

		JSONObject collisionPrefJsonObj = new JSONObject();
		collisionPrefJsonObj.put("objective", "collision");
		collisionPrefJsonObj.put("min-step-value", 0);
		collisionPrefJsonObj.put("max-step-value", 1);
		collisionPrefJsonObj.put("scaling-const", scalingConsts.get("collision"));

		JSONObject intrusivePrefJsonObj = new JSONObject();
		intrusivePrefJsonObj.put("objective", "intrusiveness");
		intrusivePrefJsonObj.put("min-step-value", MobileRobotXMDPBuilder.NON_INTRUSIVE_PENALTY);
		intrusivePrefJsonObj.put("max-step-value", MobileRobotXMDPBuilder.VERY_INTRUSIVE_PENALTY);
		intrusivePrefJsonObj.put("scaling-const", scalingConsts.get("intrusiveness"));

		JSONArray prefJsonArray = new JSONArray();
		prefJsonArray.add(timePrefJsonObj);
		prefJsonArray.add(collisionPrefJsonObj);
		prefJsonArray.add(intrusivePrefJsonObj);

		missionJsonObj.put("preference-info", prefJsonArray);
		return missionJsonObj;
	}

	private static double getMaxStepTravelTime(String mapFilename)
			throws URISyntaxException, MapTopologyException, IOException, ParseException {
		File mapJsonFile = FileIOUtils.getMapFile(MissionJSONGenerator.class, mapFilename);
		MapTopology mapTopology = MapTopologyUtils.parseMapTopology(mapJsonFile, true);
		Iterator<Connection> connIter = mapTopology.connectionIterator();
		double maxStepTime = 0;
		while (connIter.hasNext()) {
			Connection connection = connIter.next();
			double distance = connection.getDistance();
			Occlusion occlusion = connection.getConnectionAttribute(Occlusion.class, "occlusion");
			double stepTime = (distance / MobileRobotXMDPBuilder.HALF_SPEED)
					* TravelTimeQFunction.getDelayRate(occlusion);
			maxStepTime = Math.max(maxStepTime, stepTime);
		}
		return maxStepTime;
	}

	public static void main(String[] args)
			throws MapTopologyException, URISyntaxException, IOException, ParseException {
		Map<String, Double> scalingConsts = new HashMap<>();
		scalingConsts.put("travelTime", 0.3);
		scalingConsts.put("collision", 0.4);
		scalingConsts.put("intrusiveness", 0.3);
		JSONObject missionJsonObj = createMissionJsonObject("GHC7-map0.json", scalingConsts);
		File outputFile = FileIOUtils.createOutputFile("GHC7-mission0.json");
		FileIOUtils.prettyPrintJSONObjectToFile(missionJsonObj, outputFile);
	}
}
