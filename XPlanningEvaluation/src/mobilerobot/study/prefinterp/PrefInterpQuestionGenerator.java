package mobilerobot.study.prefinterp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.XPlanningOutDirectories;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import language.exceptions.XMDPException;
import language.objectives.CostCriterion;
import language.policy.Policy;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.study.utilities.QuestionViz;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.PolicyWriter;

public class PrefInterpQuestionGenerator {

	private static final int DEFAULT_NUM_MULTI_CHOICE = 5;

	private QuestionViz mQuestionViz = new QuestionViz();
	private int mNumMultiChoicePolicies;

	public PrefInterpQuestionGenerator(int numMultiChoicePolicies) {
		mNumMultiChoicePolicies = numMultiChoicePolicies;
	}

	public void generateAllPrefInterpQuestions(File mapsDir, String startNodeID, String goalNodeID)
			throws ResultParsingException, URISyntaxException, IOException, ParseException, DSMException, XMDPException,
			PrismException {
		int nextMissionIndex = 0;
		for (File mapJsonFile : mapsDir.listFiles()) {
			nextMissionIndex = generatePrefInterpQuestions(mapJsonFile, startNodeID, goalNodeID, nextMissionIndex);
		}
	}

	public int generatePrefInterpQuestions(File mapJsonFile, String startNodeID, String goalNodeID,
			int startMissionIndex) throws ResultParsingException, URISyntaxException, IOException, ParseException,
			DSMException, XMDPException, PrismException {
		LowerConvexHullPolicyCollection lowerConvexHull = new LowerConvexHullPolicyCollection(mapJsonFile, startNodeID,
				goalNodeID, startMissionIndex);
		for (Entry<File, PolicyInfo> e : lowerConvexHull) {
			File missionFile = e.getKey();
			PolicyInfo solnPolicyInfo = e.getValue();
			QuantitativePolicy solnQuantPolicy = solnPolicyInfo.getQuantitativePolicy();
			Set<QuantitativePolicy> multiChoicePolicies = lowerConvexHull
					.randomlySelectUniqueQuantitativePolicies(mNumMultiChoicePolicies, solnQuantPolicy);
			createQuestionDir(missionFile, multiChoicePolicies, solnPolicyInfo);
		}

		return lowerConvexHull.getNextMissionIndex();
	}

	private void createQuestionDir(File missionFile, Set<QuantitativePolicy> multiChoicePolicies,
			PolicyInfo solnPolicyInfo) throws IOException, ResultParsingException, PrismException, XMDPException,
			MapTopologyException, ParseException, URISyntaxException {
		File questionDir = QuestionUtils.initializeQuestionDir(missionFile);
		QuestionUtils.writeSolutionPolicyToQuestionDir(solnPolicyInfo, questionDir);

		// Write all multiple-choice policies as json files at /output/question-missionX/
		int i = 0;
		List<QuantitativePolicy> indexedMultiChoicePolicies = new ArrayList<>();
		for (QuantitativePolicy choiceQuantPolicy : multiChoicePolicies) {
			JSONObject choicePolicyJsonObj = PolicyWriter.writePolicyJSONObject(choiceQuantPolicy.getPolicy());
			String choicePolicyFilename = FileIOUtils.insertIndexToFilename("choicePolicy.json", i);
			File choicePolicyFile = FileIOUtils.createOutFile(questionDir, choicePolicyFilename);
			FileIOUtils.prettyPrintJSONObjectToFile(choicePolicyJsonObj, choicePolicyFile);
			// Keep track of index of each choice policy
			indexedMultiChoicePolicies.add(choiceQuantPolicy);
			i++;
		}

		// Compute the optimality scores of all choice policies
		JSONObject scoreCardJsonObj = computeOptimalityScores(missionFile, indexedMultiChoicePolicies, solnPolicyInfo);
		QuestionUtils.writeScoreCardToQuestionDir(scoreCardJsonObj, questionDir);

		// Visualize map of the mission and all choice policies
		mQuestionViz.visualizeAll(questionDir);
	}

	private JSONObject computeOptimalityScores(File missionFile, List<QuantitativePolicy> indexedMultiChoicePolicies,
			PolicyInfo solnPolicyInfo) throws PrismException, IOException, ResultParsingException, XMDPException {
		String missionName = FilenameUtils.removeExtension(missionFile.getName());

		XPlanningOutDirectories outputDirs = FileIOUtils.createXPlanningOutDirectories();
		Path modelOutputPath = outputDirs.getPrismModelsOutputPath().resolve(missionName);
		Path advOutputPath = outputDirs.getPrismAdvsOutputPath().resolve(missionName);

		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath.toString(),
				advOutputPath.toString());
		PrismConnector prismConnector = new PrismConnector(solnPolicyInfo.getXMDP(), CostCriterion.TOTAL_COST,
				prismConnSetttings);

		JSONObject scoreCardJsonObj = new JSONObject();
		for (int i = 0; i < indexedMultiChoicePolicies.size(); i++) {
			QuantitativePolicy choiceQuantPolicy = indexedMultiChoicePolicies.get(i);
			Policy choicePolicy = choiceQuantPolicy.getPolicy();
			// Compute cost of the choice policy, using the cost function of the solution policy
			double choicePolicyCost = prismConnector.computeObjectiveCost(choicePolicy);
			double solnPolicyCost = solnPolicyInfo.getObjectiveCost();
			double optimalityScore = solnPolicyCost / choicePolicyCost;
			String choiceName = "choicePolicy" + i;
			scoreCardJsonObj.put(choiceName, optimalityScore);
		}

		// Close down PRISM
		prismConnector.terminate();

		return scoreCardJsonObj;
	}

	public static void main(String[] args) throws URISyntaxException, ResultParsingException, IOException,
			ParseException, DSMException, XMDPException, PrismException {
		String startNodeID;
		String goalNodeID;
		if (args.length >= 2) {
			startNodeID = args[0];
			goalNodeID = args[1];
		} else {
			startNodeID = MissionJSONGenerator.DEFAULT_START_NODE_ID;
			goalNodeID = MissionJSONGenerator.DEFAULT_GOAL_NODE_ID;
		}

		File mapsDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);

		PrefInterpQuestionGenerator generator = new PrefInterpQuestionGenerator(DEFAULT_NUM_MULTI_CHOICE);
		generator.generateAllPrefInterpQuestions(mapsDir, startNodeID, goalNodeID);
	}

}
