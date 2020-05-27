package mobilerobot.study.utilities;

import java.io.File;
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
	private File mMapsResourceDir;
	private boolean mRenderMap;

	public QuestionViz(File mapsResourceDir, boolean renderMap) {
		mMapsResourceDir = mapsResourceDir;
		mRenderMap = renderMap;
	}

	public void visualizeQuestions(File questionDir) throws MapTopologyException, IOException, ParseException {
		if (mRenderMap) {
			visualizeMap(questionDir);
		}

		visualizePolicies(questionDir);
	}

	public void visualizeMap(File questionDir) throws IOException, ParseException, MapTopologyException {
		JSONObject missionJsonObj = QuestionUtils.getMissionJSONObject(questionDir);

		String mapFilename = (String) missionJsonObj.get("map-file");
		File mapJsonFile = new File(mMapsResourceDir, mapFilename);

		// Render map of the mission at /output/question-missionX/
		mMapRenderer.render(mapJsonFile, questionDir);
	}

	public void visualizePolicies(File questionDir) throws MapTopologyException, IOException, ParseException {
		// The top level of /question-mission[X]/ dir contains mission[X].json file

		// Get map-file, start-id, and goal-id from mission[X].json
		JSONObject missionJsonObj = QuestionUtils.getMissionJSONObject(questionDir);
		String startID = (String) missionJsonObj.get("start-id");
		String goalID = (String) missionJsonObj.get("goal-id");
		String mapFilename = (String) missionJsonObj.get("map-file");
		File mapJsonFile = new File(mMapsResourceDir, mapFilename);

		// Visualize all [policyName].json files in this /question-mission[X]/ dir, recursively
		visualizePoliciesRecursive(questionDir, mapJsonFile, startID, goalID);
	}

	private void visualizePoliciesRecursive(File targetDir, File mapJsonFile, String startID, String goalID)
			throws MapTopologyException, IOException, ParseException {
		// There can be 1 or more [policyName].json files in each question dir (or a target dir)
		// Excluding [policyNameValuesX].json from the filter
		File[] policyJsonFiles = FileIOUtils.listFilesWithRegexFilter(targetDir, POLICY_NAME_REGEX, ".json");

		// If there is no [policyName].json file in this dir, then go into sub dirs

		// Render all policies at /output/question-missionX/
		for (File policyJsonFile : policyJsonFiles) {
			mPolicyRenderer.render(policyJsonFile, mapJsonFile, startID, goalID, targetDir, null);
		}

		// Visualize all policies in sub-directories recursively
		// Assume that all policies are of the same map
		File[] subDirs = targetDir.listFiles(File::isDirectory);
		for (File subDir : subDirs) {
			visualizePoliciesRecursive(subDir, mapJsonFile, startID, goalID);
		}
	}

	public void visualizeAllQuestions(File questionDirOrRootDir)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		if (QuestionUtils.isQuestionDir(questionDirOrRootDir)) {
			visualizeQuestions(questionDirOrRootDir);
		} else {
			for (File questionDir : QuestionUtils.listQuestionDirs(questionDirOrRootDir)) {
				visualizeAllQuestions(questionDir);
			}
		}
	}

	public static void main(String[] args)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		String questionsRootDirname = args[0];
		File questionsRootDir = new File(questionsRootDirname);

		boolean validationFlag = args.length > 1 && args[1].equals("-v");
		boolean renderMap = args.length > 2 && args[2].equals("-m");

		File mapsResourceDir;
		if (validationFlag) {
			// Validation maps resource dir is /missiongen/validation-maps/
			mapsResourceDir = FileIOUtils.getResourceDir(MissionJSONGenerator.class, "validation-maps");
		} else {
			// Default maps resource dir is /missiongen/maps/
			mapsResourceDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);

		}

		QuestionViz questionViz = new QuestionViz(mapsResourceDir, renderMap);
		questionViz.visualizeAllQuestions(questionsRootDir);
	}
}
