package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import language.exceptions.XMDPException;
import language.policy.Policy;
import mobilerobot.study.prefinterp.LowerConvexHullPolicyCollection;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.study.utilities.QuestionViz;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.PolicyWriter;

public class PrefAlignQuestionGenerator {

	private QuestionViz mQuestionViz = new QuestionViz();

	public int generatePrefAlignQuestions(File mapJsonFile, String startNodeID, String goalNodeID,
			int startMissionIndex) throws ResultParsingException, URISyntaxException, IOException, ParseException,
			DSMException, XMDPException, PrismException {
		LowerConvexHullPolicyCollection lowerConvexHull = new LowerConvexHullPolicyCollection(mapJsonFile, startNodeID,
				goalNodeID, startMissionIndex);
		for (Entry<File, PolicyInfo> e : lowerConvexHull) {
			// Each mission is unique in terms of the scaling constants of the objective function
			File missionFile = e.getKey();
			// Each solution policy is not necessarily unique
			PolicyInfo solnPolicyInfo = e.getValue();

			// Each agent's policy is unique, taken from the lower convex hull of the mission problem
			for (QuantitativePolicy agentQuantPolicy : lowerConvexHull.getAllUniqueQuantitativePolicies()) {
				createQuestionDir(missionFile, agentQuantPolicy, solnPolicyInfo);
			}
		}
		return lowerConvexHull.getNextMissionIndex();
	}

	private void createQuestionDir(File missionFile, QuantitativePolicy agentQuantPolicy, PolicyInfo solnPolicyInfo)
			throws IOException, MapTopologyException, ParseException, URISyntaxException {
		File questionDir = QuestionUtils.initializeQuestionDir(missionFile);
		QuestionUtils.writeSolutionPolicyToQuestionDir(solnPolicyInfo, questionDir);

		// Write agent's proposed solution policy as json file at /output/question-missionX/
		JSONObject agentPolicyJsonObj = PolicyWriter.writePolicyJSONObject(agentQuantPolicy.getPolicy());
		File agentPolicyFile = FileIOUtils.createOutFile(questionDir, "agentPolicy.json");
		FileIOUtils.prettyPrintJSONObjectToFile(agentPolicyJsonObj, agentPolicyFile);

		JSONObject scoreCardJsonObj = computeAnswerScores(agentQuantPolicy, solnPolicyInfo);
		QuestionUtils.writeScoreCardToQuestionDir(scoreCardJsonObj, questionDir);

		// Visualize map of the mission and the agent's proposed policy
		mQuestionViz.visualizeAll(questionDir);
	}

	private JSONObject computeAnswerScores(QuantitativePolicy agentQuantPolicy, PolicyInfo solnPolicyInfo) {
		Policy agentPolicy = agentQuantPolicy.getPolicy();
		Policy solnPolicy = solnPolicyInfo.getPolicy();
		double yesScore = agentPolicy.equals(solnPolicy) ? 1 : 0;
		double noScore = agentPolicy.equals(solnPolicy) ? 0 : 1;

		JSONObject scoreCardJsonObj = new JSONObject();
		scoreCardJsonObj.put("yes", yesScore);
		scoreCardJsonObj.put("no", noScore);
		return scoreCardJsonObj;
	}
}
