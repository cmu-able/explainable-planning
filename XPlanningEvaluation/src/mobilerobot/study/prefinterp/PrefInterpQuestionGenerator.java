package mobilerobot.study.prefinterp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.policy.Policy;
import mobilerobot.study.prefmodels.LowerConvexHullPolicyCollection;
import mobilerobot.study.utilities.IQuestionGenerator;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.study.utilities.QuestionViz;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.PolicyWriter;

public class PrefInterpQuestionGenerator implements IQuestionGenerator {

	private static final int DEFAULT_NUM_MULTI_CHOICE = 5;

	private QuestionViz mQuestionViz;
	private int mNumMultiChoicePolicies;

	public PrefInterpQuestionGenerator(File mapsJsonDir) {
		this(mapsJsonDir, DEFAULT_NUM_MULTI_CHOICE);
	}

	public PrefInterpQuestionGenerator(File mapsJsonDir, int numMultiChoicePolicies) {
		mQuestionViz = new QuestionViz(mapsJsonDir);
		mNumMultiChoicePolicies = numMultiChoicePolicies;
	}

	@Override
	public int generateQuestions(File mapJsonFile, String startNodeID, String goalNodeID, int startMissionIndex)
			throws ResultParsingException, URISyntaxException, IOException, ParseException, DSMException, XMDPException,
			PrismException, ExplicitModelParsingException, GRBException {
		LowerConvexHullPolicyCollection lowerConvexHull = new LowerConvexHullPolicyCollection(mapJsonFile, startNodeID,
				goalNodeID, startMissionIndex);
		for (Entry<PolicyInfo, File> e : lowerConvexHull) {
			PolicyInfo solnPolicyInfo = e.getKey();
			File missionFile = e.getValue();
			QuantitativePolicy solnQuantPolicy = solnPolicyInfo.getQuantitativePolicy();
			Set<QuantitativePolicy> multiChoicePolicies = lowerConvexHull
					.randomlySelectUniqueQuantitativePolicies(mNumMultiChoicePolicies, solnQuantPolicy);
			createQuestionDir(missionFile, multiChoicePolicies, solnPolicyInfo);
		}

		return lowerConvexHull.getNextMissionIndex();
	}

	private void createQuestionDir(File missionFile, Set<QuantitativePolicy> multiChoicePolicies,
			PolicyInfo solnPolicyInfo) throws IOException, ResultParsingException, PrismException, XMDPException,
			MapTopologyException, ParseException {
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
		mQuestionViz.visualizeQuestions(questionDir);
	}

	private JSONObject computeOptimalityScores(File missionFile, List<QuantitativePolicy> indexedMultiChoicePolicies,
			PolicyInfo solnPolicyInfo) throws PrismException, IOException, ResultParsingException, XMDPException {
		PrismConnector prismConnector = QuestionUtils.createPrismConnector(missionFile, solnPolicyInfo.getXMDP());

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

}
