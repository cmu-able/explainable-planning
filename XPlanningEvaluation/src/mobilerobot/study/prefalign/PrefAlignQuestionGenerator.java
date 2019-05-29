package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import language.exceptions.XMDPException;
import mobilerobot.study.prefinterp.LowerConvexHullPolicyCollection;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.PolicyWriter;

public class PrefAlignQuestionGenerator {

	public int generatePrefAlignQuestions(File mapJsonFile, String startNodeID, String goalNodeID,
			int startMissionIndex) throws ResultParsingException, URISyntaxException, IOException, ParseException,
			DSMException, XMDPException, PrismException {
		LowerConvexHullPolicyCollection lowerConvexHull = new LowerConvexHullPolicyCollection(mapJsonFile, startNodeID,
				goalNodeID, startMissionIndex);
		for (Entry<File, PolicyInfo> e : lowerConvexHull) {
			File missionFile = e.getKey();
			PolicyInfo solnPolicyInfo = e.getValue();

			for (QuantitativePolicy quantPolicy : lowerConvexHull.getAllUniqueQuantitativePolicies()) {

			}
		}
		return lowerConvexHull.getNextMissionIndex();
	}

	private void createQuestionDir(File missionFile, QuantitativePolicy agentQuantPolicy, PolicyInfo solnPolicyInfo)
			throws IOException {
		File questionDir = QuestionUtils.initializeQuestionDir(missionFile);
		QuestionUtils.writeSolutionPolicyToQuestionDir(solnPolicyInfo, questionDir);

		// Write agent's proposed solution policy as json file at /output/question-missionX/
		JSONObject agentPolicyJsonObj = PolicyWriter.writePolicyJSONObject(agentQuantPolicy.getPolicy());
		File agentPolicyFile = FileIOUtils.createOutFile(questionDir, "agentPolicy.json");
		FileIOUtils.prettyPrintJSONObjectToFile(agentPolicyJsonObj, agentPolicyFile);
	}

}
