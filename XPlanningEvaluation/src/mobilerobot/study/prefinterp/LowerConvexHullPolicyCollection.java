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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.XPlanningOutDirectories;
import examples.mobilerobot.demo.MobileRobotDemo;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.metrics.CollisionDomain;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.IntrusivenessDomain;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.models.MoveToAction;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import language.domain.metrics.CountQFunction;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.NonStandardMetricQFunction;
import language.domain.models.IAction;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import language.policy.Policy;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.missiongen.ObjectiveInfo;
import mobilerobot.study.prefalign.SimpleCostStructure;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;

public class LowerConvexHullPolicyCollection implements Iterable<Entry<PolicyInfo, File>> {

	private static final String[] OBJECTIVE_NAMES = { TravelTimeQFunction.NAME, CollisionEvent.NAME,
			IntrusiveMoveEvent.NAME };
	private static final Double[][] PARAM_LISTS = { { 0.6, 0.3, 0.1 }, { 0.6, 0.2, 0.2 }, { 0.4, 0.4, 0.2 },
			{ 0.33, 0.33, 0.33 } };

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mMapName;

	// Lower convex hull of the map
	// Each key is a PolicyInfo containing a solution policy that lies in the LCH. The policy is not necessarily unique, 
	// but the PolicyInfo is unique due to its unique XMDP.
	// Each value is a mission file corresponding to the key's XMDP.
	private Map<PolicyInfo, File> mPolicyInfos = new HashMap<>();

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
			DSMException, XMDPException, PrismException, ParseException {
		File mapsJsonDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);
		XPlanningOutDirectories outputDirs = FileIOUtils.createXPlanningOutDirectories();
		MobileRobotDemo demo = new MobileRobotDemo(mapsJsonDir, outputDirs);
		File outputDir = FileIOUtils.getOutputDir();
		File missionsOfMapDir = new File(outputDir, "missions-of-" + mMapName);
		for (File missionJsonFile : missionsOfMapDir.listFiles()) {
			// Adjust scaling consts in mission file s.t. all QA unit costs are rounded to nearest int
			XMDP xmdp = demo.loadXMDPFromMissionFile(missionJsonFile);
			adjustMissionFile(missionJsonFile, xmdp);

			// Run planning using mission.json as input
			PolicyInfo policyInfo = demo.runPlanning(missionJsonFile);

			// Keep track of each LCH policy, its XMDP, and mission file that generates it
			mPolicyInfos.put(policyInfo, missionJsonFile);

			// Append a new unique (quantitative) policy to the list for random selection
			QuantitativePolicy quantPolicy = policyInfo.getQuantitativePolicy();
			Policy policy = quantPolicy.getPolicy();
			if (!mUniquePolicies.contains(policy)) {
				mIndexedUniqueQuantPolicies.add(quantPolicy);
				mUniquePolicies.add(policy);
			}
		}
	}

	private void adjustMissionFile(File missionJsonFile, XMDP xmdp)
			throws DSMException, XMDPException, IOException, ParseException {
		QSpace qSpace = xmdp.getQSpace();

		// Adjust cost function s.t. all QA unit costs are rounded to nearest int
		SimpleCostStructure simpleCostStruct = createSimpleCostStructure(xmdp);
		CostFunction adjustedCostFunction = simpleCostStruct.getAdjustedCostFunction();

		JSONObject missionJsonObj = FileIOUtils.readJSONObjectFromFile(missionJsonFile);
		JSONArray prefInfoJsonAry = (JSONArray) missionJsonObj.get("preference-info");
		for (Object obj : prefInfoJsonAry) {
			JSONObject prefInfoJsonObj = (JSONObject) obj;
			String qaName = (String) prefInfoJsonObj.get("objective");
			IQFunction<IAction, ITransitionStructure<IAction>> qFunction = qSpace.getQFunction(IQFunction.class,
					qaName);
			AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> attrCostFunc = adjustedCostFunction
					.getAttributeCostFunction(qFunction);
			double adjustedScalingConst = adjustedCostFunction.getScalingConstant(attrCostFunc);
			prefInfoJsonObj.put("scaling-const", adjustedScalingConst);
		}

		// Rewrite mission file with adjusted scaling consts
		FileIOUtils.prettyPrintJSONObjectToFile(missionJsonObj, missionJsonFile);
	}

	private SimpleCostStructure createSimpleCostStructure(XMDP xmdp) {
		QSpace qSpace = xmdp.getQSpace();
		TravelTimeQFunction timeQFunction = qSpace.getQFunction(TravelTimeQFunction.class, TravelTimeQFunction.NAME);
		CountQFunction<MoveToAction, CollisionDomain, CollisionEvent> collideQFunction = qSpace
				.getQFunction(CountQFunction.class, CollisionEvent.NAME);
		NonStandardMetricQFunction<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> intrusiveQFunction = qSpace
				.getQFunction(NonStandardMetricQFunction.class, IntrusiveMoveEvent.NAME);

		Map<IQFunction<?, ?>, Double> qaUnitAmounts = new HashMap<>();
		qaUnitAmounts.put(timeQFunction, 1.0); // 1 unit-time = 1 minute
		qaUnitAmounts.put(collideQFunction, 0.1); // 1 unit-collision = 0.1 E[collision]
		qaUnitAmounts.put(intrusiveQFunction, 1.0); // 1 unit-intrusiveness = 1-penalty of intrusiveness

		CostFunction costFunction = xmdp.getCostFunction();
		return new SimpleCostStructure(qaUnitAmounts, costFunction);
	}

	public int getNextMissionIndex() {
		return mNextMissionIndex;
	}

	public List<QuantitativePolicy> getIndexedUniqueQuantitativePolicies() {
		return mIndexedUniqueQuantPolicies;
	}

	/**
	 * Note that the same solution policy may be created by different mission files. Since LCH policy collection only
	 * keeps track of unique policies, this method will only return one mission file that generates a given policy.
	 * 
	 * @param quantPolicy
	 *            : Quantitative policy
	 * @return Mission file that generates the given policy
	 */
	public File getMissionFile(QuantitativePolicy quantPolicy) {
		for (Entry<PolicyInfo, File> e : mPolicyInfos.entrySet()) {
			PolicyInfo policyInfo = e.getKey();
			File missionFile = e.getValue();

			if (policyInfo.getQuantitativePolicy().equals(quantPolicy)) {
				return missionFile;
			}
		}
		throw new IllegalArgumentException("Quantitative policy argument is not in this LCH");
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
	 * Iterator over pairs of LCH PolicyInfo and its corresponding mission JSON file.
	 */
	@Override
	public Iterator<Entry<PolicyInfo, File>> iterator() {
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
