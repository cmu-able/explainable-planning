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
		JSONObject missionJsonObj = QuestionUtils.getMissionJSONObject(questionDir);

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

		JSONObject missionJsonObj = QuestionUtils.getMissionJSONObject(questionDir);
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

	public void visualizeAllQuestions(File questionDirOrRootDir)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		if (QuestionUtils.isQuestionDir(questionDirOrRootDir)) {
			visualizeQuestions(questionDirOrRootDir);
		} else {
			for (File questionDir : questionDirOrRootDir.listFiles(QuestionUtils.getQuestionDirFileFilter())) {
				visualizeAllQuestions(questionDir);
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
