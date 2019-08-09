package mobilerobot.missiongen;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.MobileRobotXMDPBuilder;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.models.Occlusion;
import mobilerobot.missiongen.ObjectiveInfo.IGetMaxStepValue;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.utilities.MapTopologyUtils;

public class MissionJSONGenerator {

	public static final String DEFAULT_START_NODE_ID = "L1";
	public static final String DEFAULT_GOAL_NODE_ID = "L32";

	private List<ObjectiveInfo> mObjectivesInfo;

	public MissionJSONGenerator(List<ObjectiveInfo> objectivesInfo) {
		mObjectivesInfo = objectivesInfo;
	}

	public Set<JSONObject> createAllMissions(File mapsDir, String startNodeID, String goalNodeID)
			throws MapTopologyException, URISyntaxException, IOException, ParseException {
		Set<JSONObject> allMissions = new HashSet<>();
		for (File mapJsonFile : mapsDir.listFiles()) {
			Set<Map<String, Integer>> combs = getAllScalingConstsCombinations();
			for (Map<String, Integer> scalingConsts : combs) {
				Map<String, Double> normScalingConsts = getNormalizedScalingConsts(scalingConsts);
				JSONObject mission = createMissionJsonObject(mapJsonFile, startNodeID, goalNodeID, normScalingConsts);
				allMissions.add(mission);
			}
		}
		return allMissions;
	}

	public JSONObject createMissionJsonObject(File mapJsonFile, String startNodeID, String goalNodeID,
			Map<String, Double> scalingConsts)
			throws URISyntaxException, MapTopologyException, IOException, ParseException {
		JSONObject missionJsonObj = new JSONObject();
		missionJsonObj.put("start-id", startNodeID);
		missionJsonObj.put("goal-id", goalNodeID);
		missionJsonObj.put("map-file", mapJsonFile.getName());

		MapTopology mapTopology = MapTopologyUtils.parseMapTopology(mapJsonFile, true);

		JSONArray prefJsonArray = new JSONArray();
		for (ObjectiveInfo objectiveInfo : mObjectivesInfo) {
			JSONObject objectivePrefJsonObj = new JSONObject();
			objectivePrefJsonObj.put("objective", objectiveInfo.getName());
			objectivePrefJsonObj.put("min-step-value", objectiveInfo.getMinStepValue());
			objectivePrefJsonObj.put("max-step-value", objectiveInfo.getMaxStepValue(mapTopology));
			objectivePrefJsonObj.put("scaling-const", scalingConsts.get(objectiveInfo.getName()));

			prefJsonArray.add(objectivePrefJsonObj);
		}
		missionJsonObj.put("preference-info", prefJsonArray);
		return missionJsonObj;
	}

	private Set<Map<String, Integer>> getAllScalingConstsCombinations() {
		String[] objectives = new String[mObjectivesInfo.size()];
		for (int i = 0; i < mObjectivesInfo.size(); i++) {
			ObjectiveInfo objectiveInfo = mObjectivesInfo.get(i);
			String objectiveName = objectiveInfo.getName();
			objectives[i] = objectiveName;
		}
		int sum = 10;
		return getAllScalingConstsCombinations(objectives, sum);
	}

	private Set<Map<String, Integer>> getAllScalingConstsCombinations(String[] objectives, int sum) {
		Set<Map<String, Integer>> combs = new HashSet<>();
		if (objectives.length == 1) {
			// Base case: only 1 objective left to assign scaling const
			// The objective's scaling const must be sum
			Map<String, Integer> scalingConst = new HashMap<>();
			scalingConst.put(objectives[0], sum);
			combs.add(scalingConst);
		} else if (objectives.length > 1) {
			// Use scaling const from 1 to highest possible value
			int scalingConst = 1;
			while (scalingConst <= sum - (objectives.length - 1)) {
				// Create a set of all possible subsets of scaling consts recursively
				String[] subObjectives = Arrays.copyOfRange(objectives, 1, objectives.length);
				int subSum = sum - scalingConst;
				Set<Map<String, Integer>> subScalingConstsSet = getAllScalingConstsCombinations(subObjectives, subSum);

				for (Map<String, Integer> subScalingConsts : subScalingConstsSet) {
					// Create a new set of scaling consts
					Map<String, Integer> scalingConsts = new HashMap<>();
					scalingConsts.put(objectives[0], scalingConst);

					// Append each subset of scaling consts to the set
					scalingConsts.putAll(subScalingConsts);

					// Add the set of (complete) scaling consts to the combinations set
					combs.add(scalingConsts);
				}

				scalingConst++;
			}
		}
		return combs;
	}

	private Map<String, Double> getNormalizedScalingConsts(Map<String, Integer> scalingConsts) {
		Map<String, Double> normScalingConsts = new HashMap<>();
		int sum = scalingConsts.values().stream().mapToInt(Integer::intValue).sum();
		for (Entry<String, Integer> e : scalingConsts.entrySet()) {
			double normScalingConst = (double) e.getValue() / sum;
			normScalingConsts.put(e.getKey(), normScalingConst);
		}
		return normScalingConsts;
	}

	private static double getMaxStepTravelTime(MapTopology mapTopology) throws MapTopologyException {
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
		String startNodeID;
		String goalNodeID;
		if (args.length > 1) {
			startNodeID = args[0];
			goalNodeID = args[1];
		} else {
			startNodeID = DEFAULT_START_NODE_ID;
			goalNodeID = DEFAULT_GOAL_NODE_ID;
		}

		File mapsDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);
		List<ObjectiveInfo> objectivesInfo = getDefaultObjectivesInfo();

		MissionJSONGenerator missionGen = new MissionJSONGenerator(objectivesInfo);
		Set<JSONObject> missionJsonObjs = missionGen.createAllMissions(mapsDir, startNodeID, goalNodeID);
		writeMissionsToJSONFiles(missionJsonObjs, 0);
	}

	public static List<ObjectiveInfo> getDefaultObjectivesInfo() {
		IGetMaxStepValue getMaxStepTravelTime = new IGetMaxStepValue() {

			@Override
			public double getMaxStepValue(MapTopology mapTopology) throws MapTopologyException {
				return getMaxStepTravelTime(mapTopology);
			}
		};

		ObjectiveInfo timeInfo = new ObjectiveInfo(TravelTimeQFunction.NAME, 0, getMaxStepTravelTime);
		ObjectiveInfo collisionInfo = new ObjectiveInfo(CollisionEvent.NAME, 0, 1);
		ObjectiveInfo intrusiveInfo = new ObjectiveInfo(IntrusiveMoveEvent.NAME,
				MobileRobotXMDPBuilder.NON_INTRUSIVE_PENALTY, MobileRobotXMDPBuilder.VERY_INTRUSIVE_PENALTY);

		List<ObjectiveInfo> objectivesInfo = new ArrayList<>();
		objectivesInfo.add(timeInfo);
		objectivesInfo.add(collisionInfo);
		objectivesInfo.add(intrusiveInfo);

		return objectivesInfo;
	}

	/**
	 * Write missions as .json files to /output/, with filenames starting from the given starting index.
	 * 
	 * @param missionJsonObjs
	 * @param startMissionIndex
	 * @return Next mission index
	 * @throws IOException
	 */
	public static final int writeMissionsToJSONFiles(Set<JSONObject> missionJsonObjs, int startMissionIndex)
			throws IOException {
		int i = startMissionIndex;
		for (JSONObject missionJsonObj : missionJsonObjs) {
			String mapFilename = (String) missionJsonObj.get("map-file");
			String mapName = FilenameUtils.removeExtension(mapFilename);
			String outSubDirname = "missions-of-" + mapName;
			String outFilename = FileIOUtils.insertIndexToFilename("mission.json", i);
			File outFile = FileIOUtils.createOutputFile(outSubDirname, outFilename);
			FileIOUtils.prettyPrintJSONObjectToFile(missionJsonObj, outFile);
			i++;
		}
		return i;
	}
}
