package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;

import examples.common.DSMException;
import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.demo.MobileRobotXPlanner;
import explanation.analysis.QuantitativePolicy;
import explanation.verbalization.QADecimalFormatter;
import explanation.verbalization.VerbalizerSettings;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;
import uiconnector.ExplanationWriter;
import uiconnector.PolicyWriter;

public class PrefAlignAgentGenerator {

	private MobileRobotXPlanner mXPlanner;
	private XPlannerOutDirectories mOutputDirs;

	public PrefAlignAgentGenerator(File mapsJsonDir) throws IOException {
		XPlannerOutDirectories outputDirs = FileIOUtils.createXPlannerOutDirectories();

		// Configure verbalizer settings
		VerbalizerSettings verbalizerSettings = new VerbalizerSettings();
		verbalizerSettings.setDescribeCosts(false);
		verbalizerSettings.setQADecimalFormatter(MobileRobotXPlanner.getQADecimalFormatter());
		MobileRobotXPlanner.setVerbalizerOrdering(verbalizerSettings);

		mXPlanner = new MobileRobotXPlanner(mapsJsonDir, outputDirs, verbalizerSettings);
		mOutputDirs = outputDirs;
	}

	public MobileRobotXPlanner getXPlanner() {
		return mXPlanner;
	}

	public void writeAgentPolicyAndValues(File questionDir, QuantitativePolicy agentQuantPolicy, int agentIndex)
			throws IOException {
		// Write each agent's proposed solution policy as json file at /output/question-missionX/
		JSONObject agentPolicyJsonObj = PolicyWriter.writePolicyJSONObject(agentQuantPolicy.getPolicy());
		writeAgentJSONObjectToFile(agentPolicyJsonObj, "agentPolicy.json", agentIndex, questionDir);

		QADecimalFormatter decimalFormatter = MobileRobotXPlanner.getQADecimalFormatter();

		// Write the QA values of each agent policy as json file
		JSONObject agentPolicyValuesJsonObj = ExplanationWriter.writeQAValuesToJSONObject(agentQuantPolicy,
				decimalFormatter);
		writeAgentJSONObjectToFile(agentPolicyValuesJsonObj, "agentPolicyValues.json", agentIndex, questionDir);
	}

	private static void writeAgentJSONObjectToFile(JSONObject agentJsonObj, String filename, int agentIndex,
			File questionDir) throws IOException {
		String agentFilename = FileIOUtils.insertIndexToFilename(filename, agentIndex);
		File agentFile = FileIOUtils.createOutFile(questionDir, agentFilename);
		FileIOUtils.prettyPrintJSONObjectToFile(agentJsonObj, agentFile);
	}

	public void createAgentExplanationDir(File questionDir, File agentMissionFile, int agentIndex)
			throws IOException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException {
		// Create explanation sub-directory for each agent: /question-mission[X]/explanation-agent[i]/
		String explanationDirname = "explanation-agent" + agentIndex;
		File explanationDir = FileIOUtils.createOutSubDir(questionDir, explanationDirname);

		// Copy mission[Y].json file (corresponding mission of agent[i], which may or may not be mission[X].json)
		// from its directory (e.g., /missions-of-{mapName}/ or /validation-missions/) to the explanation sub-dir
		// Note: mission[Y] name is unique across all maps
		Files.copy(agentMissionFile.toPath(), explanationDir.toPath().resolve(agentMissionFile.getName()));

		// Check if mission[Y] of agent[i] has already been explained
		// If so, no need to run XPlanning on mission[Y].json again
		Path explanationsOutPath = mOutputDirs.getExplanationsOutputPath();
		String agentMissionName = FilenameUtils.removeExtension(agentMissionFile.getName());
		String agentExplanationFilename = agentMissionName + "_explanation.json";
		Path agentExplanationPath = explanationsOutPath.resolve(agentExplanationFilename);

		if (!agentExplanationPath.toFile().exists()) {
			// mission[Y]_explanation.json does not exist -- mission[Y] has not been explained yet
			// Run XPlanning on mission[Y].json to create explanation for agent[i]'s policy
			mXPlanner.runXPlanning(agentMissionFile);
		}

		// Copy solution policy, alternative policies, and explanation from XPlanningOutDirectories to the explanation
		// sub-dir:
		// Copy solnPolicy.json and all altPolicy[j].json from /[XPlannerOutDir]/policies/mission[Y]/ to the explanation sub-dir
		Path policiesOutPath = mOutputDirs.getPoliciesOutputPath();
		Path agentMissionPoliciesOutPath = policiesOutPath.resolve(agentMissionName);
		FileUtils.copyDirectory(agentMissionPoliciesOutPath.toFile(), explanationDir);

		// Copy mission[Y]_explanation.json from /[XPlannerOutDir]/explanations/ to the explanation sub-dir
		Files.copy(agentExplanationPath, explanationDir.toPath().resolve(agentExplanationFilename));
	}
}
