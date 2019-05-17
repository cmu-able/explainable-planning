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
import java.util.Random;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.Directories;
import examples.mobilerobot.demo.MobileRobotDemo;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.PolicyInfo;
import language.exceptions.XMDPException;
import language.policy.Policy;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.missiongen.ObjectiveInfo;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.utilities.MissionJSONParserUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;

public class LowerConvexHullPolicyCollection implements Iterable<PolicyInfo> {

	private static final String[] OBJECTIVE_NAMES = { "travelTime", "collision", "intrusiveness" };
	private static final Double[][] PARAM_LISTS = { { 0.6, 0.3, 0.1 }, { 0.6, 0.2, 0.2 }, { 0.4, 0.4, 0.2 },
			{ 0.33, 0.33, 0.33 } };

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<WADDPattern, PolicyInfo> mPolicyInfos = new HashMap<>();

	// For random policy selection: indexed unique policies
	private List<Policy> mIndexedUniquePolicies = new ArrayList<>();

	// For random policy selection: random number generator with seed
	private Random mRandom = new Random(0L);

	public LowerConvexHullPolicyCollection(File mapJsonFile, String startNodeID, String goalNodeID)
			throws URISyntaxException, IOException, ParseException, ResultParsingException, DSMException, XMDPException,
			PrismException {
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

	private void populateLowerConvexHullPolicies() throws URISyntaxException, IOException, ResultParsingException,
			DSMException, XMDPException, PrismException, ParseException {
		File mapsJsonDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);
		Directories outputDirs = FileIOUtils.createXPlanningDirectories();
		MobileRobotDemo demo = new MobileRobotDemo(mapsJsonDir, outputDirs);
		File missionsDir = FileIOUtils.getMissionsResourceDir(getClass());
		for (File missionJsonFile : missionsDir.listFiles()) {
			PolicyInfo policyInfo = demo.runPlanning(missionJsonFile);

			JSONObject missionJsonObj = FileIOUtils.readJSONObjectFromFile(missionJsonFile);
			Map<String, Double> scalingConsts = MissionJSONParserUtils.parseScalingConsts(missionJsonObj);
			WADDPattern waddPattern = new WADDPattern();
			waddPattern.putAllWeights(scalingConsts);

			mPolicyInfos.put(waddPattern, policyInfo);

			// Append a new unique policy to the list for random selection
			Policy policy = policyInfo.getPolicy();
			if (!mIndexedUniquePolicies.contains(policy)) {
				mIndexedUniquePolicies.add(policy);
			}
		}
	}

	public PolicyInfo getOptimalPolicyInfo(WADDPattern waddPattern) {
		return mPolicyInfos.get(waddPattern);
	}

	public Set<Policy> randomlySelectUniquePolicies(int numPolicies) {
		Set<Policy> uniqueRandomPolicies = new HashSet<>();
		for (int i = 0; i < numPolicies; i++) {
			Policy randomPolicy;
			do {
				int policyIndex = mRandom.nextInt(mIndexedUniquePolicies.size());
				randomPolicy = mIndexedUniquePolicies.get(policyIndex);
			} while (uniqueRandomPolicies.contains(randomPolicy));

			uniqueRandomPolicies.add(randomPolicy);
		}
		return uniqueRandomPolicies;
	}

	@Override
	public Iterator<PolicyInfo> iterator() {
		return mPolicyInfos.values().iterator();
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
