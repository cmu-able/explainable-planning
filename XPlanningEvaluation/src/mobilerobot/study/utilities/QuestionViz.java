package mobilerobot.study.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.policyviz.MapRenderer;
import mobilerobot.policyviz.PolicyRenderer;
import mobilerobot.utilities.FileIOUtils;

public class QuestionViz {

	private static final String QUESTION_DIRNAME_REGEX = "question-mission[0-9]+";
	private static final String MISSION_NAME_REGEX = "mission[0-9]+";
	private static final String POLICY_NAME_REGEX = ".*policy[0-9]*";

	private MapRenderer mMapRenderer = new MapRenderer();
	private PolicyRenderer mPolicyRenderer = new PolicyRenderer();

	public void visualizeQuestions(File questionDir)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		File mapJsonFile = visualizeMap(questionDir);
		visualizePolicies(questionDir, mapJsonFile);
	}

	public File visualizeMap(File questionDir)
			throws IOException, ParseException, URISyntaxException, MapTopologyException {
		JSONObject missionJsonObj = getMissionJsonObject(questionDir);

		String mapFilename = (String) missionJsonObj.get("map-file");
		File mapJsonFile = FileIOUtils.getMapFile(MissionJSONGenerator.class, mapFilename);

		// Render map of the mission at /output/question-missionX/
		mMapRenderer.render(mapJsonFile, questionDir);

		return mapJsonFile;
	}

	public void visualizePolicies(File questionDir, File mapJsonFile)
			throws MapTopologyException, IOException, ParseException {
		// There can be 1 or more [policyName].json files in each question dir
		// Excluding [policyNameValuesX].json from the filter
		File[] policyJsonFiles = FileIOUtils.listFilesWithRegexFilter(questionDir, POLICY_NAME_REGEX, ".json");

		JSONObject missionJsonObj = getMissionJsonObject(questionDir);
		String startID = (String) missionJsonObj.get("start-id");
		String goalID = (String) missionJsonObj.get("goal-id");

		// Render all policies at /output/question-missionX/
		for (File policyJsonFile : policyJsonFiles) {
			mPolicyRenderer.render(policyJsonFile, mapJsonFile, startID, goalID, questionDir, null);
		}

		// Visualize all policies in sub-directories recursively
		// Assume that all policies are of the same map
		FileFilter subDirFileFilter = File::isDirectory;
		for (File subDir : questionDir.listFiles(subDirFileFilter)) {
			visualizePolicies(subDir, mapJsonFile);
		}
	}

	private JSONObject getMissionJsonObject(File questionDir) throws IOException, ParseException {
		File[] missionJsonFiles = FileIOUtils.listFilesWithRegexFilter(questionDir, MISSION_NAME_REGEX, ".json");
		// There is only 1 missionX.json file in each question dir
		File missionJsonFile = missionJsonFiles[0];
		return FileIOUtils.readJSONObjectFromFile(missionJsonFile);
	}

	public void visualizeAllQuestions(File questionDirOrRootDir)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		if (questionDirOrRootDir.getName().matches(QUESTION_DIRNAME_REGEX)) {
			visualizeQuestions(questionDirOrRootDir);
		} else {
			FileFilter dirFilter = File::isDirectory;
			for (File subDir : questionDirOrRootDir.listFiles(dirFilter)) {
				visualizeAllQuestions(subDir);
			}
		}
	}

	public static void main(String[] args)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		String questionsRootDirname = args[0];
		File questionsRootDir = new File(questionsRootDirname);

		QuestionViz questionViz = new QuestionViz();
		questionViz.visualizeAllQuestions(questionsRootDir);
	}
}
