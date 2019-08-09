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
	private File mMapsResourceDir;

	public QuestionViz(File mapsResourceDir) {
		mMapsResourceDir = mapsResourceDir;
	}

	public void visualizeQuestions(File questionDir) throws MapTopologyException, IOException, ParseException {
		File mapJsonFile = visualizeMap(questionDir);
		visualizePolicies(questionDir, mapJsonFile);
	}

	public File visualizeMap(File questionDir) throws IOException, ParseException, MapTopologyException {
		JSONObject missionJsonObj = QuestionUtils.getMissionJSONObject(questionDir);

		String mapFilename = (String) missionJsonObj.get("map-file");
		File mapJsonFile = new File(mMapsResourceDir, mapFilename);

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
			for (File questionDir : QuestionUtils.listQuestionDirs(questionDirOrRootDir)) {
				visualizeAllQuestions(questionDir);
			}
		}
	}

	public static void main(String[] args)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		String questionsRootDirname = args[0];
		File questionsRootDir = new File(questionsRootDirname);

		File mapsResourceDir;
		if (args.length > 1 && args[1].equals("-v")) {
			// Validation maps resource dir is /missiongen/validation-maps/
			mapsResourceDir = FileIOUtils.getResourceDir(MissionJSONGenerator.class, "validation-maps");
		} else {
			// Default maps resource dir is /missiongen/maps/
			mapsResourceDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);

		}

		QuestionViz questionViz = new QuestionViz(mapsResourceDir);
		questionViz.visualizeAllQuestions(questionsRootDir);
	}
}
