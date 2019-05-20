package mobilerobot.study.prefinterp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import explanation.analysis.PolicyInfo;
import language.exceptions.XMDPException;
import language.policy.Policy;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.PolicyWriter;

public class PrefInterpQuestionGenerator {

	private static final int DEFAULT_NUM_MULTI_CHOICE = 5;

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
			Policy solnPolicy = solnPolicyInfo.getPolicy();
			Set<Policy> multiChoicePolicies = lowerConvexHull.randomlySelectUniquePolicies(mNumMultiChoicePolicies,
					solnPolicy);
			createQuestionDir(missionFile, multiChoicePolicies, solnPolicy);
		}

		return lowerConvexHull.getNextMissionIndex();
	}

	private void createQuestionDir(File missionFile, Set<Policy> multiChoicePolicies, Policy solnPolicy)
			throws IOException {
		File outputDir = FileIOUtils.getOutputDir();
		String missionName = FilenameUtils.removeExtension(missionFile.getName());

		// Create question sub-directory: /output/question-missionX/
		String questionSubDirname = "question-" + missionName;
		File questionSubDir = FileIOUtils.createOutSubDir(outputDir, questionSubDirname);

		// Copy missionX.json file from /output/missions-of-{mapName}/ to /output/question-missionX/
		// Note: missionX name is unique across all maps
		Files.copy(missionFile.toPath(), questionSubDir.toPath().resolve(missionFile.getName()));

		// Write all multiple-choice policies as json files at /output/question-missionX/
		int i = 0;
		for (Policy choicePolicy : multiChoicePolicies) {
			JSONObject choicePolicyJsonObj = PolicyWriter.writePolicyJSONObject(choicePolicy);
			String choicePolicyFilename = FileIOUtils.insertIndexToFilename("choicePolicy.json", i);
			File choicePolicyFile = FileIOUtils.createOutFile(questionSubDir, choicePolicyFilename);
			FileIOUtils.prettyPrintJSONObjectToFile(choicePolicyJsonObj, choicePolicyFile);
			i++;
		}

		// Write the solution policy as json file at /output/question-missionX/
		JSONObject solnPolicyJsonObj = PolicyWriter.writePolicyJSONObject(solnPolicy);
		File solnPolicyFile = FileIOUtils.createOutFile(questionSubDir, "solnPolicy.json");
		FileIOUtils.prettyPrintJSONObjectToFile(solnPolicyJsonObj, solnPolicyFile);
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
