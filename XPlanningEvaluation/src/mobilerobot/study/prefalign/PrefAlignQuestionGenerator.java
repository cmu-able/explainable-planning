package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.demo.MobileRobotDemo;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import explanation.verbalization.QADecimalFormatter;
import explanation.verbalization.VerbalizerSettings;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.XMDPException;
import language.policy.Policy;
import mobilerobot.study.prefmodels.LowerConvexHullPolicyCollection;
import mobilerobot.study.prefmodels.SimpleCostStructure;
import mobilerobot.study.utilities.IQuestionGenerator;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.study.utilities.QuestionViz;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.xplanning.XPlanningRunner;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.ExplanationWriter;
import uiconnector.PolicyWriter;

public class PrefAlignQuestionGenerator implements IQuestionGenerator {

	private static final double EQUALITY_TOL = 1e-5;

	private QuestionViz mQuestionViz = new QuestionViz();
	private XPlanningRunner mXPlanningRunner;
	private VerbalizerSettings mVerbalizerSettings = new VerbalizerSettings();

	public PrefAlignQuestionGenerator(File mapsJsonDir) throws IOException {
		XPlannerOutDirectories outputDirs = FileIOUtils.createXPlannerOutDirectories();
		mXPlanningRunner = new XPlanningRunner(mapsJsonDir, outputDirs);
		mVerbalizerSettings.setDescribeCosts(false);
		mVerbalizerSettings.setQADecimalFormatter(MobileRobotDemo.getQADecimalFormatter());
		MobileRobotDemo.setVerbalizerOrdering(mVerbalizerSettings);
	}

	@Override
	public int generateQuestions(File mapJsonFile, String startNodeID, String goalNodeID, int startMissionIndex)
			throws URISyntaxException, IOException, ParseException, DSMException, XMDPException, PrismException,
			PrismConnectorException, GRBException {
		LowerConvexHullPolicyCollection lowerConvexHull = new LowerConvexHullPolicyCollection(mapJsonFile, startNodeID,
				goalNodeID, startMissionIndex);
		for (Entry<PolicyInfo, File> e : lowerConvexHull) {
			// Each solution PolicyInfo is unique due to its XMDP, but the solution policy is not necessarily unique
			PolicyInfo solnPolicyInfo = e.getKey();
			// Each mission is unique in terms of the scaling constants of the objective function
			File missionFile = e.getValue();

			// Each question dir contains multiple questions, all of which have the same mission but different agent's
			// proposed policies
			File questionDir = QuestionUtils.initializeQuestionDir(missionFile);
			QuestionUtils.writeSolutionPolicyToQuestionDir(solnPolicyInfo, questionDir);

			// Each question dir has a simpleCostStructure.json defining the preference
			SimpleCostStructure simpleCostStruct = lowerConvexHull.getSimpleCostStructure(missionFile);
			createSimpleCostStructureFile(questionDir, simpleCostStruct);

			List<QuantitativePolicy> indexedAgentQuantPolicies = new ArrayList<>();

			// One of the agents in this question dir must be solving this question's XMDP
			// This agent will generate its explanation using the mission file in this question
			QuantitativePolicy solnQuantPolicy = solnPolicyInfo.getQuantitativePolicy();

			// Agent-0 is the aligned agent
			createAgentData(questionDir, solnQuantPolicy, 0, missionFile);
			indexedAgentQuantPolicies.add(0, solnQuantPolicy);

			// The rest of the agents in this question dir are unique and different from agent-0,
			// taken from the lower convex hull of the mission problem
			List<QuantitativePolicy> indexedLCHQuantPolicies = lowerConvexHull.getIndexedUniqueQuantitativePolicies();

			Iterator<QuantitativePolicy> iter = indexedLCHQuantPolicies.iterator();
			// The rest of the agents start at index 1
			int agentIndex = 1;
			while (iter.hasNext()) {
				QuantitativePolicy agentQuantPolicy = iter.next();

				if (agentQuantPolicy.getPolicy().equals(solnQuantPolicy.getPolicy())) {
					// This agent policy is the same as the aligned agent policy
					// Skip this
					continue;
				}

				// Note: The same agent policy may be created by different mission files,
				// but we can use any of those mission files
				// because a non-aligned agent doesn't need to have a specific cost function
				File agentMissionFile = lowerConvexHull.getMissionFile(agentQuantPolicy);

				// Create agentPolicy[i].json, agentPolicyValues[i].json, and explanation-agent[i]
				createAgentData(questionDir, agentQuantPolicy, agentIndex, agentMissionFile);

				indexedAgentQuantPolicies.add(agentIndex, agentQuantPolicy);
				agentIndex++;
			}

			// Create answer key for all questions as answerKey.json at /output/question-missionX/
			JSONObject answerKeyJsonObj = createAnswerKey(missionFile, indexedAgentQuantPolicies, solnPolicyInfo);
			File answerKeyFile = FileIOUtils.createOutFile(questionDir, "answerKey.json");
			FileIOUtils.prettyPrintJSONObjectToFile(answerKeyJsonObj, answerKeyFile);

			// Compute optimality scores of all agent policies
			JSONObject scoreCardJsonObj = computeOptimalityScores(missionFile, indexedAgentQuantPolicies,
					solnPolicyInfo);
			QuestionUtils.writeScoreCardToQuestionDir(scoreCardJsonObj, questionDir);

			// Visualize map of the mission, each agent's proposed policy, and all policies in the explanation of each
			// agent
			mQuestionViz.visualizeQuestions(questionDir);
		}
		return lowerConvexHull.getNextMissionIndex();
	}

	private void createAgentData(File questionDir, QuantitativePolicy agentQuantPolicy, int agentIndex,
			File agentMissionFile)
			throws IOException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException {
		// Write each agent's proposed solution policy as json file at /output/question-missionX/
		JSONObject agentPolicyJsonObj = PolicyWriter.writePolicyJSONObject(agentQuantPolicy.getPolicy());
		writeAgentJSONObjectToFile(agentPolicyJsonObj, "agentPolicy.json", agentIndex, questionDir);

		QADecimalFormatter decimalFormatter = mVerbalizerSettings.getQADecimalFormatter();

		// Write the QA values of each agent policy as json file
		JSONObject agentPolicyValuesJsonObj = ExplanationWriter.writeQAValuesToJSONObject(agentQuantPolicy,
				decimalFormatter);
		writeAgentJSONObjectToFile(agentPolicyValuesJsonObj, "agentPolicyValues.json", agentIndex, questionDir);

		// Create explanation dir that contains agent's corresponding mission file, solution policy, alternative
		// policies, and explanation at /output/question-missionX/explanation-agent[i]/
		createExplanation(questionDir, agentIndex, agentMissionFile);
	}

	private void writeAgentJSONObjectToFile(JSONObject agentJsonObj, String filename, int agentIndex, File questionDir)
			throws IOException {
		String agentFilename = FileIOUtils.insertIndexToFilename(filename, agentIndex);
		File agentFile = FileIOUtils.createOutFile(questionDir, agentFilename);
		FileIOUtils.prettyPrintJSONObjectToFile(agentJsonObj, agentFile);
	}

	private void createSimpleCostStructureFile(File questionDir, SimpleCostStructure simpleCostStruct)
			throws IOException {
		JSONObject simpleCostStructJsonObj = new JSONObject();

		for (IQFunction<?, ?> qFunction : simpleCostStruct.getAdjustedCostFunction().getQFunctions()) {
			String descriptiveUnit = simpleCostStruct.getDescriptiveUnit(qFunction);
			double unitCost = simpleCostStruct.getRoundedSimplestCostOfEachUnit(qFunction);

			String formattedUnitCost = simpleCostStruct.formatUnitCost(unitCost);
			JSONObject unitCostJsonObj = new JSONObject();
			unitCostJsonObj.put("unit", descriptiveUnit);
			unitCostJsonObj.put("cost", formattedUnitCost);
			simpleCostStructJsonObj.put(qFunction.getName(), unitCostJsonObj);
		}

		File simpleCostStructFile = FileIOUtils.createOutFile(questionDir, "simpleCostStructure.json");
		FileIOUtils.prettyPrintJSONObjectToFile(simpleCostStructJsonObj, simpleCostStructFile);
	}

	private void createExplanation(File questionDir, int agentIndex, File agentMissionFile)
			throws IOException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException {
		// Create explanation sub-directory for each agent: /output/question-missionX/explanation-agent[i]/
		String explanationDirname = "explanation-agent" + agentIndex;
		File explanationDir = FileIOUtils.createOutSubDir(questionDir, explanationDirname);

		// Copy missionY.json file (corresponding mission of agent[i]) from /output/missions-of-{mapName}/ to the
		// explanation sub-dir
		// Note: missionY name is unique across all maps
		Files.copy(agentMissionFile.toPath(), explanationDir.toPath().resolve(agentMissionFile.getName()));

		// Check if missionY of agent[i] has already been explained
		// If so, no need to run XPlanning on missionY.json again
		XPlannerOutDirectories xplannerOutDirs = mXPlanningRunner.getXPlannerOutDirectories();
		Path explanationsOutPath = xplannerOutDirs.getExplanationsOutputPath();
		String agentMissionName = FilenameUtils.removeExtension(agentMissionFile.getName());
		String agentExplanationFilename = agentMissionName + "_explanation.json";
		Path agentExplanationPath = explanationsOutPath.resolve(agentExplanationFilename);

		if (!agentExplanationPath.toFile().exists()) {
			// missionY_explanation.json does not exist -- missionY has not been explained yet
			// Run XPlanning on missionY.json to create explanation for agent[i]'s policy
			mXPlanningRunner.runMission(agentMissionFile, mVerbalizerSettings);
		}

		// Copy solution policy, alternative policies, and explanation from XPlanningOutDirectories to the explanation
		// sub-dir

		// Copy solnPolicy.json and all altPolicy[j].json from /output/policies/missionY/ to the explanation sub-dir
		Path policiesOutPath = xplannerOutDirs.getPoliciesOutputPath();
		Path agentMissionPoliciesOutPath = policiesOutPath.resolve(agentMissionName);
		FileUtils.copyDirectory(agentMissionPoliciesOutPath.toFile(), explanationDir);

		// Copy missionY_explanation.json from /output/explanations/ to the explanation sub-dir
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

	private JSONObject computeOptimalityScores(File missionFile, List<QuantitativePolicy> indexedAgentQuantPolicies,
			PolicyInfo solnPolicyInfo) throws IOException, PrismException, ResultParsingException, XMDPException {
		PrismConnector prismConnector = QuestionUtils.createPrismConnector(missionFile, solnPolicyInfo.getXMDP());

		JSONObject scoreCardJsonObj = new JSONObject();
		for (int i = 0; i < indexedAgentQuantPolicies.size(); i++) {
			QuantitativePolicy agentQuantPolicy = indexedAgentQuantPolicies.get(i);
			Policy agentPolicy = agentQuantPolicy.getPolicy();

			// Compute cost of the agent policy, using the cost function of the solution policy
			double agentPolicyCost = prismConnector.computeObjectiveCost(agentPolicy);
			double solnPolicyCost = solnPolicyInfo.getObjectiveCost();
			double optimalityScore = solnPolicyCost / agentPolicyCost;
			String agentPolicyName = "agentPolicy" + i;
			scoreCardJsonObj.put(agentPolicyName, optimalityScore);
		}

		// Close down PRISM
		prismConnector.terminate();

		return scoreCardJsonObj;
	}
}
