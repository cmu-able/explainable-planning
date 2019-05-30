package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import language.exceptions.XMDPException;
import language.policy.Policy;
import mobilerobot.study.prefinterp.LowerConvexHullPolicyCollection;
import mobilerobot.study.utilities.IQuestionGenerator;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.study.utilities.QuestionViz;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.PolicyWriter;

public class PrefAlignQuestionGenerator implements IQuestionGenerator {

	private QuestionViz mQuestionViz = new QuestionViz();

	@Override
	public int generateQuestions(File mapJsonFile, String startNodeID, String goalNodeID, int startMissionIndex)
			throws ResultParsingException, URISyntaxException, IOException, ParseException, DSMException, XMDPException,
			PrismException {
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

			// Each agent's policy is unique, taken from the lower convex hull of the mission problem
			List<QuantitativePolicy> indexedAgentQuantPolicies = lowerConvexHull.getIndexedUniqueQuantitativePolicies();
			for (int i = 0; i < indexedAgentQuantPolicies.size(); i++) {
				QuantitativePolicy agentQuantPolicy = indexedAgentQuantPolicies.get(i);

				// Write each agent's proposed solution policy as json file at /output/question-missionX/
				JSONObject agentPolicyJsonObj = PolicyWriter.writePolicyJSONObject(agentQuantPolicy.getPolicy());
				String agentPolicyFilename = FileIOUtils.insertIndexToFilename("agentPolicy.json", i);
				File agentPolicyFile = FileIOUtils.createOutFile(questionDir, agentPolicyFilename);
				FileIOUtils.prettyPrintJSONObjectToFile(agentPolicyJsonObj, agentPolicyFile);

				// TODO: Create a question file with easily-interpretable cost structure
			}

			// Create answer key for all questions as scoreCard.json at /output/question-missionX/
			JSONObject answerKeyJsonObj = createAnswerKey(indexedAgentQuantPolicies, solnPolicyInfo);
			QuestionUtils.writeScoreCardToQuestionDir(answerKeyJsonObj, questionDir);

			// Visualize map of the mission and the agent's proposed policy
			mQuestionViz.visualizeAll(questionDir);
		}
		return lowerConvexHull.getNextMissionIndex();
	}

	private JSONObject createAnswerKey(List<QuantitativePolicy> indexedAgentQuantPolicies, PolicyInfo solnPolicyInfo) {
		JSONObject answerKeyJsonObj = new JSONObject();

		for (int i = 0; i < indexedAgentQuantPolicies.size(); i++) {
			QuantitativePolicy agentQuantPolicy = indexedAgentQuantPolicies.get(i);
			Policy agentPolicy = agentQuantPolicy.getPolicy();
			Policy solnPolicy = solnPolicyInfo.getPolicy();
			String agentPolicyName = "agentPolicy" + i;
			String answer = agentPolicy.equals(solnPolicy) ? "yes" : "no";
			answerKeyJsonObj.put(agentPolicyName, answer);
		}

		return answerKeyJsonObj;
	}
}
