package mobilerobot.study.prefinterp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.PolicyInfo;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.missiongen.ObjectiveInfo;
import mobilerobot.utilities.FileIOUtils;

public class LowerConvexHullPolicyCollection implements Iterable<PolicyInfo> {

	private static final String[] OBJECTIVE_NAMES = { "travelTime", "collision", "intrusiveness" };
	private static final Double[][] PARAM_LISTS = { { 0.6, 0.3, 0.1 }, { 0.6, 0.2, 0.2 }, { 0.4, 0.4, 0.2 },
			{ 0.33, 0.33, 0.33 } };

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private List<PolicyInfo> mPolicyInfos = new ArrayList<>();

	public LowerConvexHullPolicyCollection(File mapJsonFile, String startNodeID, String goalNodeID)
			throws MapTopologyException, URISyntaxException, IOException, ParseException {
		createMissionJSONFiles(mapJsonFile, startNodeID, goalNodeID);
		populateLowerConvexHullPolicies();
	}

	private void createMissionJSONFiles(File mapJsonFile, String startNodeID, String goalNodeID)
			throws MapTopologyException, URISyntaxException, IOException, ParseException {
		List<ObjectiveInfo> objectivesInfo = MissionJSONGenerator.getDefaultObjectivesInfo();
		MissionJSONGenerator missionGenerator = new MissionJSONGenerator(objectivesInfo);

		Set<JSONObject> missionJsonObjs = new HashSet<>();

		PrefPatternCollection prefPatternColl = new PrefPatternCollection(OBJECTIVE_NAMES, PARAM_LISTS);
		for (WADDPattern waddPattern : prefPatternColl) {
			Map<String, Double> scalingConsts = convertWADDToScalingConsts(waddPattern);

			// Create a mission for each WADD pattern
			JSONObject missionJsonObj = missionGenerator.createMissionJsonObject(mapJsonFile, startNodeID, goalNodeID,
					scalingConsts);
			missionJsonObjs.add(missionJsonObj);
		}

		File missionsDir = FileIOUtils.getMissionsResourceDir(getClass());
		MissionJSONGenerator.writeMissionsToJSONFiles(missionsDir, missionJsonObjs);
	}

	private Map<String, Double> convertWADDToScalingConsts(WADDPattern waddPattern) {
		Map<String, Double> scalingConsts = new HashMap<>();
		for (String objectiveName : OBJECTIVE_NAMES) {
			scalingConsts.put(objectiveName, waddPattern.getWeight(objectiveName));
		}
		return scalingConsts;
	}

	private void populateLowerConvexHullPolicies() {
		// TODO
	}

	@Override
	public Iterator<PolicyInfo> iterator() {
		return mPolicyInfos.iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof LowerConvexHullPolicyCollection)) {
			return false;
		}
		LowerConvexHullPolicyCollection coll = (LowerConvexHullPolicyCollection) obj;
		return coll.mPolicyInfos.equals(mPolicyInfos);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPolicyInfos.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
