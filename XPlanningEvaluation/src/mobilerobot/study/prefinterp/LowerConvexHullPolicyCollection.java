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
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.XPlanningOutDirectories;
import examples.mobilerobot.demo.MobileRobotDemo;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import language.exceptions.XMDPException;
import language.policy.Policy;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.missiongen.ObjectiveInfo;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;

public class LowerConvexHullPolicyCollection implements Iterable<Entry<File, PolicyInfo>> {

	private static final String[] OBJECTIVE_NAMES = { "travelTime", "collision", "intrusiveness" };
	private static final Double[][] PARAM_LISTS = { { 0.6, 0.3, 0.1 }, { 0.6, 0.2, 0.2 }, { 0.4, 0.4, 0.2 },
			{ 0.33, 0.33, 0.33 } };

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mMapName;

	// Lower convex hull of the map
	// Each key is a mission whose optimal solution lies in the lower convex hull, and each value is that solution
	// Each mission is unique in terms of the scaling constants of the objective function, but each solution is not
	// necessarily unique.
	private Map<File, PolicyInfo> mPolicyInfos = new HashMap<>();

	// For random policy selection: indexed unique quantitative policies
	// Note: All of these policies are generated from the same map but different objective cost functions,
	// so comparing QA values across these policies is legitimate.
	private List<QuantitativePolicy> mIndexedUniqueQuantPolicies = new ArrayList<>();

	// To keep track of unique policies, since quantitative policies may not have consistent precision of QA values
	private Set<Policy> mUniquePolicies = new HashSet<>();

	// For random policy selection: random number generator with seed
	private Random mRandom = new Random(0L);

	private int mNextMissionIndex;

	public LowerConvexHullPolicyCollection(File mapJsonFile, String startNodeID, String goalNodeID,
			int startMissionIndex) throws URISyntaxException, IOException, ParseException, ResultParsingException,
			DSMException, XMDPException, PrismException {
		mMapName = FilenameUtils.removeExtension(mapJsonFile.getName());
		createMissionJSONFiles(mapJsonFile, startNodeID, goalNodeID, startMissionIndex);
		populateLowerConvexHullPolicies();
	}

	private void createMissionJSONFiles(File mapJsonFile, String startNodeID, String goalNodeID, int startMissionIndex)
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

		mNextMissionIndex = MissionJSONGenerator.writeMissionsToJSONFiles(missionJsonObjs, startMissionIndex);
	}

	private Map<String, Double> convertWADDToScalingConsts(WADDPattern waddPattern) {
		Map<String, Double> scalingConsts = new HashMap<>();
		for (String objectiveName : OBJECTIVE_NAMES) {
			scalingConsts.put(objectiveName, waddPattern.getWeight(objectiveName));
		}
		return scalingConsts;
	}

	private void populateLowerConvexHullPolicies() throws URISyntaxException, IOException, ResultParsingException,
			DSMException, XMDPException, PrismException {
		File mapsJsonDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);
		XPlanningOutDirectories outputDirs = FileIOUtils.createXPlanningOutDirectories();
		MobileRobotDemo demo = new MobileRobotDemo(mapsJsonDir, outputDirs);
		File outputDir = FileIOUtils.getOutputDir();
		File missionsOfMapDir = new File(outputDir, "missions-of-" + mMapName);
		for (File missionJsonFile : missionsOfMapDir.listFiles()) {
			// Run planning using mission.json as input
			PolicyInfo policyInfo = demo.runPlanning(missionJsonFile);

			// Keep track of mission file that generates each lower-convex-hull policy
			mPolicyInfos.put(missionJsonFile, policyInfo);

			// Append a new unique (quantitative) policy to the list for random selection
			QuantitativePolicy quantPolicy = policyInfo.getQuantitativePolicy();
			Policy policy = quantPolicy.getPolicy();
			if (!mUniquePolicies.contains(policy)) {
				mIndexedUniqueQuantPolicies.add(quantPolicy);
				mUniquePolicies.add(policy);
			}
		}
	}

	public int getNextMissionIndex() {
		return mNextMissionIndex;
	}

	public List<QuantitativePolicy> getIndexedUniqueQuantitativePolicies() {
		return mIndexedUniqueQuantPolicies;
	}

	public Set<QuantitativePolicy> randomlySelectUniqueQuantitativePolicies(int maxNumPolicies,
			QuantitativePolicy iniQuantPolicy) {
		Set<QuantitativePolicy> uniqueRandomQuantPolicies = new HashSet<>();
		uniqueRandomQuantPolicies.add(iniQuantPolicy);
		int numPolicies = Math.min(maxNumPolicies, mIndexedUniqueQuantPolicies.size());

		for (int i = 1; i < numPolicies; i++) {
			QuantitativePolicy randomQuantPolicy;
			do {
				int policyIndex = mRandom.nextInt(mIndexedUniqueQuantPolicies.size());
				randomQuantPolicy = mIndexedUniqueQuantPolicies.get(policyIndex);
			} while (uniqueRandomQuantPolicies.contains(randomQuantPolicy));

			uniqueRandomQuantPolicies.add(randomQuantPolicy);
		}
		return uniqueRandomQuantPolicies;
	}

	/**
	 * Iterator over pairs of mission JSON file and its solution policy info.
	 */
	@Override
	public Iterator<Entry<File, PolicyInfo>> iterator() {
		return mPolicyInfos.entrySet().iterator();
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
		return coll.mMapName.equals(mMapName) && coll.mPolicyInfos.equals(mPolicyInfos);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMapName.hashCode();
			result = 31 * result + mPolicyInfos.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
