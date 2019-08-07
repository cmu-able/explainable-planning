package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.missiongen.ObjectiveInfo;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.utilities.MapTopologyUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;

public class PrefAlignValidationQuestionGenerator {

	private static final String SIMPLE_COST_STRUCTURE_JSON = "simpleCostStructure.json";

	private File mMapsJsonDir;
	private PrefAlignAgentGenerator mAgentGenerator;

	public PrefAlignValidationQuestionGenerator(File mapsJsonDir) throws IOException {
		mMapsJsonDir = mapsJsonDir;
		mAgentGenerator = new PrefAlignAgentGenerator(mapsJsonDir);
	}

	public File[][] generateValidationMissionFiles(LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions,
			int startMissionIndex) throws MapTopologyException, IOException, ParseException, URISyntaxException {
		File[] validationMapFiles = mMapsJsonDir.listFiles();
		// All validation missions in a row will have the same cost structure
		File[][] validationMissionFiles = new File[allLinkedPrefAlignQuestions.length][validationMapFiles.length];
		int missionIndex = startMissionIndex;

		List<ObjectiveInfo> objectivesInfo = MissionJSONGenerator.getDefaultObjectivesInfo();
		MissionJSONGenerator missionGenerator = new MissionJSONGenerator(objectivesInfo);

		for (int i = 0; i < allLinkedPrefAlignQuestions.length; i++) {
			LinkedPrefAlignQuestions linkedQuestions = allLinkedPrefAlignQuestions[i];

			// All PrefAlign questions in a link have the same cost structure
			File costStructJsonFile = new File(linkedQuestions.getQuestionDir(0), SIMPLE_COST_STRUCTURE_JSON);
			JSONObject costStructJsonObj = FileIOUtils.readJSONObjectFromFile(costStructJsonFile);

			for (int j = 0; j < validationMapFiles.length; j++) {
				File mapJsonFile = validationMapFiles[j];
				MapTopology mapTopology = MapTopologyUtils.parseMapTopology(mapJsonFile, true);

				Map<String, Double> scalingConsts = new HashMap<>();
				for (ObjectiveInfo objectiveInfo : objectivesInfo) {
					JSONObject unitCostJsonObj = (JSONObject) costStructJsonObj.get(objectiveInfo.getName());
					String descriptiveUnit = (String) unitCostJsonObj.get("unit");
					String formattedUnitCost = (String) unitCostJsonObj.get("cost");
					double qaUnitAmount = Double.parseDouble(descriptiveUnit.split(" ")[0]);
					int qaUnitCost = Integer.parseInt(formattedUnitCost);

					double scalingConst = qaUnitCost * objectiveInfo.getMaxStepValue(mapTopology) / qaUnitAmount;
					scalingConsts.put(objectiveInfo.getName(), scalingConst);
				}

				JSONObject missionJsonObj = missionGenerator.createMissionJsonObject(mapJsonFile,
						MissionJSONGenerator.DEFAULT_START_NODE_ID, MissionJSONGenerator.DEFAULT_GOAL_NODE_ID,
						scalingConsts);

				// All validation missions files are outputted to /output/validation-missions/
				String missionFilename = FileIOUtils.insertIndexToFilename("mission.json", missionIndex);
				File missionFile = FileIOUtils.createOutputFile("validation-missions", missionFilename);
				FileIOUtils.prettyPrintJSONObjectToFile(missionJsonObj, missionFile);
				missionIndex++;

				// Each row i of validation missions corresponds to a LinkedPrefAlignQuestions at index i
				validationMissionFiles[i][j] = missionFile;
			}
		}

		return validationMissionFiles;
	}

	public void generateValidationQuestions(File[][] validationMissionFiles,
			LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions)
			throws IOException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException {
		for (int i = 0; i < validationMissionFiles.length; i++) {
			LinkedPrefAlignQuestions linkedQuestions = allLinkedPrefAlignQuestions[i];

			for (int j = 0; j < validationMissionFiles[0].length; j++) {
				File validationMissonFile = validationMissionFiles[i][j];

				// Create a question dir, /question-mission[X]/, for each validation mission
				// Each question dir contains multiple questions, all of which have the same mission but different agent's
				// proposed policies
				File validationQuestionDir = QuestionUtils.initializeQuestionDir(validationMissonFile);

				// Run xplanning on the validation mission, and write solution policy to /question-mission[X]/solnPolicy.json
				PolicyInfo solnPolicyInfo = mAgentGenerator.getXPlanner().runXPlanning(validationMissonFile);
				QuestionUtils.writeSolutionPolicyToQuestionDir(solnPolicyInfo, validationQuestionDir);

				// Create an aligned agent[0]
				// Write agentPolicy0.json and agentPolicyValues0.json to /question-mission[X]/
				mAgentGenerator.writeAgentPolicyAndValues(validationQuestionDir, solnPolicyInfo.getQuantitativePolicy(),
						0);

				// Create explanation dir that contains agent's mission file, solution policy, alternative
				// policies, and explanation at /question-mission[X]/explanation-agent[i]/
				mAgentGenerator.createAgentExplanationDir(validationQuestionDir, validationMissonFile, 0);

				// Copy simpleCostStructure.json from linked questions to /question-mission[X]/
				copySimpleCostStructureFile(linkedQuestions, validationQuestionDir);
			}
		}
	}

	private void copySimpleCostStructureFile(LinkedPrefAlignQuestions linkedQuestions, File validationQuestionDir)
			throws IOException {
		File srcDir = linkedQuestions.getQuestionDir(0);
		Path costStructSrcPath = srcDir.toPath().resolve(SIMPLE_COST_STRUCTURE_JSON);
		Path costStructDestPath = validationQuestionDir.toPath().resolve(SIMPLE_COST_STRUCTURE_JSON);
		Files.copy(costStructSrcPath, costStructDestPath);
	}
}
