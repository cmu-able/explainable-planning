package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.PolicyInfo;
import explanation.analysis.QuantitativePolicy;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.missiongen.ObjectiveInfo;
import mobilerobot.policyviz.PolicyRenderer;
import mobilerobot.study.utilities.QuestionUtils;
import mobilerobot.study.utilities.QuestionViz;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.utilities.MapTopologyUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;

public class PrefAlignValidationQuestionGenerator {

	private static final String SIMPLE_COST_STRUCTURE_JSON = "simpleCostStructure.json";

	private File mValidationMapsDir;
	private LinkedPrefAlignQuestions[] mAllLinkedPrefAlignQuestions;
	private PrefAlignAgentGenerator mAgentGenerator;
	private QuestionViz mQuestionViz;

	public PrefAlignValidationQuestionGenerator(File validationMapsDir,
			LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions) throws IOException {
		mValidationMapsDir = validationMapsDir;
		mAllLinkedPrefAlignQuestions = allLinkedPrefAlignQuestions;
		mAgentGenerator = new PrefAlignAgentGenerator(validationMapsDir);
		mQuestionViz = new QuestionViz(validationMapsDir, true);
	}

	public File[][] generateValidationMissionFiles(int startMissionIndex)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		File[] validationMapFiles = mValidationMapsDir.listFiles();
		int numLinks = mAllLinkedPrefAlignQuestions.length;
		int numValidationMissions = validationMapFiles.length;

		// All validation missions in a row will have the same cost structure
		File[][] validationMissionFiles = new File[numLinks][numValidationMissions];
		int missionIndex = startMissionIndex;

		List<ObjectiveInfo> objectivesInfo = MissionJSONGenerator.getDefaultObjectivesInfo();
		MissionJSONGenerator missionGenerator = new MissionJSONGenerator(objectivesInfo);

		for (int i = 0; i < numLinks; i++) {
			LinkedPrefAlignQuestions linkedQuestions = mAllLinkedPrefAlignQuestions[i];

			// All PrefAlign questions in a link have the same cost structure
			File costStructJsonFile = new File(linkedQuestions.getQuestionDir(0), SIMPLE_COST_STRUCTURE_JSON);
			JSONObject costStructJsonObj = FileIOUtils.readJSONObjectFromFile(costStructJsonFile);

			for (int j = 0; j < numValidationMissions; j++) {
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

	public void generateValidationQuestions(File[][] validationMissionFiles, File validationPoliciesDir)
			throws IOException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException,
			ParseException {
		int numLinks = validationMissionFiles.length;
		int numValidationMissions = validationMissionFiles[0].length;

		for (int i = 0; i < numLinks; i++) {
			LinkedPrefAlignQuestions linkedQuestions = mAllLinkedPrefAlignQuestions[i];

			for (int j = 0; j < numValidationMissions; j++) {
				File validationMissionFile = validationMissionFiles[i][j];

				// Each validation question dir is created at /output/question-mission[X]/

				// Create a question dir, /question-mission[X]/, for each validation mission
				// Each question dir contains multiple questions, all of which have the same mission but different agent's
				// proposed policies
				File validationQuestionDir = QuestionUtils.initializeQuestionDir(validationMissionFile, true);

				// Copy simpleCostStructure.json from linked questions to /question-mission[X]/
				copySimpleCostStructureFile(linkedQuestions, validationQuestionDir);

				// Create aligned agent and unaligned agent(s)
				PolicyInfo solnPolicyInfo = createAlignedAgent(validationQuestionDir, validationMissionFile);
				List<QuantitativePolicy> unalignedAgentQuantPolicies = createUnalignedAgents(validationQuestionDir,
						validationMissionFile, validationPoliciesDir);

				List<QuantitativePolicy> indexedAgentQuantPolicies = new ArrayList<>();
				indexedAgentQuantPolicies.add(solnPolicyInfo.getQuantitativePolicy());
				indexedAgentQuantPolicies.addAll(unalignedAgentQuantPolicies);

				PrefAlignQuestionGenerator.writeAnswerKeyAndScoreCard(validationQuestionDir, validationMissionFile,
						indexedAgentQuantPolicies, solnPolicyInfo);

				mQuestionViz.visualizeQuestions(validationQuestionDir);
			}
		}
	}

	private PolicyInfo createAlignedAgent(File validationQuestionDir, File validationMissionFile)
			throws DSMException, XMDPException, PrismException, IOException, GRBException, PrismConnectorException {
		// Run xplanning on the validation mission, and write solution policy to /question-mission[X]/solnPolicy.json
		PolicyInfo solnPolicyInfo = mAgentGenerator.computeAlignedAgentPolicyInfo(validationMissionFile);
		QuestionUtils.writeSolutionPolicyToQuestionDir(solnPolicyInfo, validationQuestionDir);

		// Create an aligned agent[0]
		// Write agentPolicy0.json and agentPolicyValues0.json to /question-mission[X]/
		mAgentGenerator.writeAgentPolicyAndValues(validationQuestionDir, solnPolicyInfo.getQuantitativePolicy(), 0);

		// Create explanation dir that contains agent's mission file, solution policy, alternative
		// policies, and explanation at /question-mission[X]/explanation-agent[i]/
		mAgentGenerator.createAgentExplanationDir(validationQuestionDir, validationMissionFile, 0);

		return solnPolicyInfo;
	}

	private List<QuantitativePolicy> createUnalignedAgents(File validationQuestionDir, File validationMissionFile,
			File validationPoliciesDir)
			throws ResultParsingException, DSMException, XMDPException, IOException, ParseException, PrismException {
		List<QuantitativePolicy> unalignedAgentQuantPolicies = new ArrayList<>();

		int unalignedAgentIndex = 1; // unaligned agents start at index 1
		for (File policyJsonFile : validationPoliciesDir.listFiles()) {
			// Create an unaligned agent[i]
			// Write agentPolicy[i].json and agentPolicyValues[i].json to /question-mission[X]/
			PolicyInfo unalignedAgentPolicyInfo = mAgentGenerator.computeUnalignedAgentPolicyInfo(validationMissionFile,
					policyJsonFile);
			QuantitativePolicy unalignedAgentQuantPolicy = unalignedAgentPolicyInfo.getQuantitativePolicy();
			mAgentGenerator.writeAgentPolicyAndValues(validationQuestionDir, unalignedAgentQuantPolicy,
					unalignedAgentIndex);

			// No explanation of unaligned agent for validation mission

			unalignedAgentQuantPolicies.add(unalignedAgentQuantPolicy);
			unalignedAgentIndex++;
		}

		return unalignedAgentQuantPolicies;
	}

	private void copySimpleCostStructureFile(LinkedPrefAlignQuestions linkedQuestions, File validationQuestionDir)
			throws IOException {
		File srcDir = linkedQuestions.getQuestionDir(0);
		Path costStructSrcPath = srcDir.toPath().resolve(SIMPLE_COST_STRUCTURE_JSON);
		Path costStructDestPath = validationQuestionDir.toPath().resolve(SIMPLE_COST_STRUCTURE_JSON);
		Files.copy(costStructSrcPath, costStructDestPath);
	}

	public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException,
			ParseException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException {
		int startMissionIndex = Integer.parseInt(args[0]);
		File validationMapsDir = FileIOUtils.getResourceDir(MissionJSONGenerator.class, "validation-maps");
		File validationPoliciesDir = FileIOUtils.getResourceDir(PolicyRenderer.class, "validation-policies");

		// Read serialized LinkedPrefAlignQuestions objects that do not contain validation questions
		File serLinkedQuestionsDir = FileIOUtils.getResourceDir(PrefAlignQuestionLinker.class,
				"serialized-linked-questions");
		LinkedPrefAlignQuestions[] allLinkedPrefAlignQuestions = PrefAlignQuestionLinker
				.readAllLinkedPrefAlignQuestions(serLinkedQuestionsDir);

		PrefAlignValidationQuestionGenerator generator = new PrefAlignValidationQuestionGenerator(validationMapsDir,
				allLinkedPrefAlignQuestions);
		File[][] validationMissionFiles = generator.generateValidationMissionFiles(startMissionIndex);
		generator.generateValidationQuestions(validationMissionFiles, validationPoliciesDir);
	}
}
