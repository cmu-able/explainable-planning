package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
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
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;

public class PrefAlignQuestionGenerator implements IQuestionGenerator {

	private static final double EQUALITY_TOL = 1e-5;

	private PrefAlignAgentGenerator mAgentGenerator;
	private QuestionViz mQuestionViz;

	public PrefAlignQuestionGenerator(File mapsJsonDir) throws IOException {
		mAgentGenerator = new PrefAlignAgentGenerator(mapsJsonDir);
		mQuestionViz = new QuestionViz(mapsJsonDir);
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
			File questionDir = QuestionUtils.initializeQuestionDir(missionFile, true);
			QuestionUtils.writeSolutionPolicyToQuestionDir(solnPolicyInfo, questionDir);

			// Each question dir has a simpleCostStructure.json defining the preference
			SimpleCostStructure simpleCostStruct = lowerConvexHull.getSimpleCostStructure(missionFile);
			createSimpleCostStructureFile(questionDir, simpleCostStruct);

			List<QuantitativePolicy> indexedAgentQuantPolicies = new ArrayList<>();

			// One of the agents in this question dir must be solving this question's XMDP
			// This agent will generate its explanation using the mission file in this question
			QuantitativePolicy solnQuantPolicy = solnPolicyInfo.getQuantitativePolicy();

			// Agent-0 is the aligned agent
			createAgentData(questionDir, solnPolicyInfo, solnQuantPolicy, missionFile, 0);
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
				createAgentData(questionDir, solnPolicyInfo, agentQuantPolicy, agentMissionFile, agentIndex);

				indexedAgentQuantPolicies.add(agentIndex, agentQuantPolicy);
				agentIndex++;
			}

			// Create answerKey.json and scoreCard.json
			writeAnswerKeyAndScoreCard(questionDir, missionFile, indexedAgentQuantPolicies, solnPolicyInfo);

			// Visualize map of the mission, each agent's proposed policy, and all policies in the explanation of each
			// agent
			mQuestionViz.visualizeQuestions(questionDir);
		}
		return lowerConvexHull.getNextMissionIndex();
	}

	private void createAgentData(File questionDir, PolicyInfo solnPolicyInfo, QuantitativePolicy agentQuantPolicy,
			File agentMissionFile, int agentIndex) throws IOException, PrismException, XMDPException,
			PrismConnectorException, GRBException, DSMException, ParseException {
		mAgentGenerator.writeAgentPolicyAndValues(questionDir, agentQuantPolicy, agentIndex);
		File agentExplanationDir = mAgentGenerator.createAgentExplanationDir(questionDir, agentMissionFile, agentIndex);
		File missionFile = QuestionUtils.getMissionJSONFile(questionDir);
		JSONObject scoreCardJsonObj = mAgentGenerator.computeAlignmentScores(missionFile, solnPolicyInfo,
				agentExplanationDir);
		// Write scoreCard.json for the agent's solution and alternative policies to the agent's explanation dir (NOT question dir)
		QuestionUtils.writeScoreCardToQuestionDir(scoreCardJsonObj, agentExplanationDir);
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

	static void writeAnswerKeyAndScoreCard(File questionDir, File missionFile,
			List<QuantitativePolicy> indexedAgentQuantPolicies, PolicyInfo solnPolicyInfo)
			throws ResultParsingException, IOException, PrismException, XMDPException {
		// Create answer key for all questions as answerKey.json at /output/question-missionX/
		JSONObject answerKeyJsonObj = createAnswerKey(missionFile, indexedAgentQuantPolicies, solnPolicyInfo);
		File answerKeyFile = FileIOUtils.createOutFile(questionDir, "answerKey.json");
		FileIOUtils.prettyPrintJSONObjectToFile(answerKeyJsonObj, answerKeyFile);

		// Compute alignment scores of all agent policies
		JSONObject scoreCardJsonObj = computeAlignmentScores(missionFile, indexedAgentQuantPolicies, solnPolicyInfo);
		QuestionUtils.writeScoreCardToQuestionDir(scoreCardJsonObj, questionDir);
	}

	private static JSONObject createAnswerKey(File missionFile, List<QuantitativePolicy> indexedAgentQuantPolicies,
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

	private static JSONObject computeAlignmentScores(File missionFile,
			List<QuantitativePolicy> indexedAgentQuantPolicies, PolicyInfo solnPolicyInfo)
			throws IOException, PrismException, ResultParsingException, XMDPException {
		PrismConnector prismConnector = QuestionUtils.createPrismConnector(missionFile, solnPolicyInfo.getXMDP());

		JSONObject scoreCardJsonObj = new JSONObject();
		for (int i = 0; i < indexedAgentQuantPolicies.size(); i++) {
			QuantitativePolicy agentQuantPolicy = indexedAgentQuantPolicies.get(i);
			Policy agentPolicy = agentQuantPolicy.getPolicy();

			// Compute cost of the agent policy, using the cost function of the solution policy
			double agentPolicyCost = prismConnector.computeObjectiveCost(agentPolicy);
			double solnPolicyCost = solnPolicyInfo.getObjectiveCost();
			double alignmentScore = solnPolicyCost / agentPolicyCost;
			String agentPolicyName = "agentPolicy" + i;
			scoreCardJsonObj.put(agentPolicyName, alignmentScore);
		}

		// Close down PRISM
		prismConnector.terminate();

		return scoreCardJsonObj;
	}
}
