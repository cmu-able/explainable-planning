package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.XPlanningOutDirectories;
import examples.mobilerobot.metrics.CollisionDomain;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.IntrusivenessDomain;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.models.MoveToAction;
import explanation.analysis.EventBasedQAValue;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import gurobi.GRBException;
import language.domain.metrics.CountQFunction;
import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.NonStandardMetricQFunction;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.objectives.CostFunction;
import language.policy.Policy;
import mobilerobot.study.prefinterp.LowerConvexHullPolicyCollection;
import mobilerobot.study.utilities.IQuestionGenerator;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.study.utilities.QuestionViz;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.xplanning.XPlanningRunner;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.PolicyWriter;

public class PrefAlignQuestionGenerator implements IQuestionGenerator {

	private static final double EQUALITY_TOL = 1e-5;

	private QuestionViz mQuestionViz = new QuestionViz();
	private XPlanningRunner mXPlanningRunner;

	public PrefAlignQuestionGenerator(File mapsJsonDir) throws IOException {
		XPlanningOutDirectories outputDirs = FileIOUtils.createXPlanningOutDirectories();
		mXPlanningRunner = new XPlanningRunner(mapsJsonDir, outputDirs);
	}

	@Override
	public int generateQuestions(File mapJsonFile, String startNodeID, String goalNodeID, int startMissionIndex)
			throws URISyntaxException, IOException, ParseException, DSMException, XMDPException, PrismException,
			PrismConnectorException, GRBException {
		LowerConvexHullPolicyCollection lowerConvexHull = new LowerConvexHullPolicyCollection(mapJsonFile, startNodeID,
				goalNodeID, startMissionIndex);
		for (Entry<File, PolicyInfo> e : lowerConvexHull) {
			// Each mission is unique in terms of the scaling constants of the objective function
			File missionFile = e.getKey();
			// Each solution policy is not necessarily unique
			PolicyInfo solnPolicyInfo = e.getValue();

			// Each question dir contains multiple questions, all of which have the same mission but different agent's
			// proposed policies
			File questionDir = QuestionUtils.initializeQuestionDir(missionFile);
			QuestionUtils.writeSolutionPolicyToQuestionDir(solnPolicyInfo, questionDir);

			// Each question dir has a simple cost structure defining the preference
			createSimpleCostStructure(questionDir, solnPolicyInfo.getXMDP());

			// Each agent's policy is unique, taken from the lower convex hull of the mission problem
			List<QuantitativePolicy> indexedAgentQuantPolicies = lowerConvexHull.getIndexedUniqueQuantitativePolicies();
			for (int i = 0; i < indexedAgentQuantPolicies.size(); i++) {
				QuantitativePolicy agentQuantPolicy = indexedAgentQuantPolicies.get(i);

				// Write each agent's proposed solution policy as json file at /output/question-missionX/
				JSONObject agentPolicyJsonObj = PolicyWriter.writePolicyJSONObject(agentQuantPolicy.getPolicy());
				writeAgentJSONObjectToFile(agentPolicyJsonObj, "agentPolicy.json", i, questionDir);

				// Write the QA values of each agent policy as json file
				JSONObject agentPolicyValuesJsonObj = createAgentPolicyValues(agentQuantPolicy);
				writeAgentJSONObjectToFile(agentPolicyValuesJsonObj, "agentPolicyValues.json", i, questionDir);

				// Create explanation dir that contains agent's corresponding mission file, solution policy, alternative
				// policies, and explanation at /output/question-missionX/explanation-agentY/
				File agentMissionFile = lowerConvexHull.getMissionFile(agentQuantPolicy);
				createExplanation(questionDir, i, agentMissionFile);
			}

			// Create answer key for all questions as scoreCard.json at /output/question-missionX/
			JSONObject answerKeyJsonObj = createAnswerKey(missionFile, indexedAgentQuantPolicies, solnPolicyInfo);
			QuestionUtils.writeScoreCardToQuestionDir(answerKeyJsonObj, questionDir);

			// Visualize map of the mission and the agent's proposed policy
			mQuestionViz.visualizeAll(questionDir);
		}
		return lowerConvexHull.getNextMissionIndex();
	}

	private void writeAgentJSONObjectToFile(JSONObject agentJsonObj, String filename, int agentIndex, File questionDir)
			throws IOException {
		String agentFilename = FileIOUtils.insertIndexToFilename(filename, agentIndex);
		File agentFile = FileIOUtils.createOutFile(questionDir, agentFilename);
		FileIOUtils.prettyPrintJSONObjectToFile(agentJsonObj, agentFile);
	}

	private void createSimpleCostStructure(File questionDir, XMDP xmdp) throws IOException {
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
		SimpleCostStructure costStruct = new SimpleCostStructure(qaUnitAmounts, costFunction);
		double unitTimeCost = costStruct.getScaledCostOfEachUnit(timeQFunction);
		double unitCollisionCost = costStruct.getScaledCostOfEachUnit(collideQFunction);
		double unitIntrusiveCost = costStruct.getScaledCostOfEachUnit(intrusiveQFunction);

		JSONObject costStructJsonObj = new JSONObject();
		costStructJsonObj.put("1 minute", unitTimeCost);
		costStructJsonObj.put("0.1 E[collision]", unitCollisionCost);
		costStructJsonObj.put("1 intrusive-penalty", unitIntrusiveCost);

		File costStructFile = FileIOUtils.createOutFile(questionDir, "simpleCostStructure.json");
		FileIOUtils.prettyPrintJSONObjectToFile(costStructJsonObj, costStructFile);
	}

	private JSONObject createAgentPolicyValues(QuantitativePolicy agentQuantPolicy) {
		JSONObject agentPolicyValuesJsonObj = new JSONObject();
		for (IQFunction<?, ?> qFunction : agentQuantPolicy) {
			if (qFunction instanceof NonStandardMetricQFunction<?, ?, ?>) {
				NonStandardMetricQFunction<?, ?, ?> eventBasedQFunction = (NonStandardMetricQFunction<?, ?, ?>) qFunction;
				EventBasedQAValue<?> eventBasedQAValue = agentQuantPolicy.getEventBasedQAValue(eventBasedQFunction);

				JSONObject agentEventBasedValuesJsonObj = new JSONObject();
				for (Entry<? extends IEvent<?, ?>, Double> e : eventBasedQAValue) {
					IEvent<?, ?> event = e.getKey();
					Double expectedCount = e.getValue();
					agentEventBasedValuesJsonObj.put(event.getName(), expectedCount);
				}
				agentPolicyValuesJsonObj.put(eventBasedQFunction.getName(), agentEventBasedValuesJsonObj);
			} else {
				double expectedValue = agentQuantPolicy.getQAValue(qFunction);
				agentPolicyValuesJsonObj.put(qFunction.getName(), expectedValue);
			}
		}
		return agentPolicyValuesJsonObj;
	}

	private void createExplanation(File questionDir, int agentIndex, File agentMissionFile)
			throws IOException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException {
		// Create explanation sub-directory for each agent: /output/question-missionX/explanation-agentY/
		String explanationDirname = "explanation-agent" + agentIndex;
		File explanationDir = FileIOUtils.createOutSubDir(questionDir, explanationDirname);

		// Copy missionY.json file (corresponding mission of agent) from /output/missions-of-{mapName}/ to the
		// explanation sub-dir
		// Note: missionY name is unique across all maps
		Files.copy(agentMissionFile.toPath(), explanationDir.toPath().resolve(agentMissionFile.getName()));

		// Run XPlanning on missionY.json to create explanation for agentY's policy
		mXPlanningRunner.runMission(agentMissionFile);

		// Copy solution policy, alternative policies, and explanation from XPlanningOutDirectories to the explanation
		// sub-dir
		XPlanningOutDirectories xplanningOutDirs = mXPlanningRunner.getXPlanningOutDirectories();

		// Copy solnPolicy.json and all altPolicy[i].json from /output/policies/missionY/ to the explanation sub-dir
		Path policiesOutPath = xplanningOutDirs.getPoliciesOutputPath();
		String agentMissionName = FilenameUtils.removeExtension(agentMissionFile.getName());
		Path agentMissionPoliciesOutPath = policiesOutPath.resolve(agentMissionName);
		FileUtils.copyDirectory(agentMissionPoliciesOutPath.toFile(), explanationDir);

		// Copy missionY_explanation.json from /output/explanations/ to the explanation sub-dir
		Path explanationsOutPath = xplanningOutDirs.getExplanationsOutputPath();
		String agentExplanationFilename = agentMissionName + "_explanation.json";
		Path agentExplanationPath = explanationsOutPath.resolve(agentExplanationFilename);
		Files.copy(agentExplanationPath, explanationDir.toPath().resolve(agentExplanationFilename));
	}

	private JSONObject createAnswerKey(File missionFile, List<QuantitativePolicy> indexedAgentQuantPolicies,
			PolicyInfo solnPolicyInfo) throws IOException, PrismException, ResultParsingException, XMDPException {
		PrismConnector prismConnector = QuestionUtils.createPrismConnector(missionFile, solnPolicyInfo.getXMDP());

		JSONObject answerKeyJsonObj = new JSONObject();
		for (int i = 0; i < indexedAgentQuantPolicies.size(); i++) {
			QuantitativePolicy agentQuantPolicy = indexedAgentQuantPolicies.get(i);
			Policy agentPolicy = agentQuantPolicy.getPolicy();
			Policy solnPolicy = solnPolicyInfo.getPolicy();

			// Agent's proposed policy is aligned if:
			// it is the same as the solution policy, OR
			// its cost (using the cost function of the solution policy) is approximately equal to the solution policy's
			// cost.
			// The latter is the compensatory case.
			String answer;
			if (agentPolicy.equals(solnPolicy)) {
				answer = "yes";
			} else {
				// Compute cost of the agent's proposed policy, using the cost function of the solution policy
				double agentPolicyCost = prismConnector.computeObjectiveCost(agentPolicy);
				double solnPolicyCost = solnPolicyInfo.getObjectiveCost();

				if (Math.abs(agentPolicyCost - solnPolicyCost) <= EQUALITY_TOL) {
					// Compensatory case: there are multiple different optimal policies to the cost function
					answer = "yes";
				} else {
					answer = "no";
				}
			}

			String agentPolicyName = "agentPolicy" + i;
			answerKeyJsonObj.put(agentPolicyName, answer);
		}

		// Close down PRISM
		prismConnector.terminate();

		return answerKeyJsonObj;
	}
}
