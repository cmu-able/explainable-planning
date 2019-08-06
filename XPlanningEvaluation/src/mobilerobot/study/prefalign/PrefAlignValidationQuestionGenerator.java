package mobilerobot.study.prefalign;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.demo.MobileRobotDemo;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.verbalization.VerbalizerSettings;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.missiongen.ObjectiveInfo;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.utilities.MapTopologyUtils;

public class PrefAlignValidationQuestionGenerator {

	private File mMapsJsonDir;
	private MobileRobotDemo mXPlanner;

	public PrefAlignValidationQuestionGenerator(File mapsJsonDir) throws IOException {
		mMapsJsonDir = mapsJsonDir;

		XPlannerOutDirectories outputDirs = FileIOUtils.createXPlannerOutDirectories();
		VerbalizerSettings verbalizerSettings = new VerbalizerSettings();
		verbalizerSettings.setDescribeCosts(false);
		verbalizerSettings.setQADecimalFormatter(MobileRobotDemo.getQADecimalFormatter());
		MobileRobotDemo.setVerbalizerOrdering(verbalizerSettings);

		mXPlanner = new MobileRobotDemo(mapsJsonDir, outputDirs, verbalizerSettings);
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
			File costStructJsonFile = new File(linkedQuestions.getQuestionDir(0), "simpleCostStruture.json");
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

	public void generateValidationQuestions(File[][] validationMissionFiles) {
		for (int i = 0; i < validationMissionFiles.length; i++) {
			for (int j = 0; j < validationMissionFiles[0].length; j++) {
				File validationMissonFile = validationMissionFiles[i][j];
			}
		}
	}
}
