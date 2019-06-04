package mobilerobot.study.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
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

	private MapRenderer mMapRenderer = new MapRenderer();
	private PolicyRenderer mPolicyRenderer = new PolicyRenderer();

	public void visualizeAll(File questionDir)
			throws MapTopologyException, IOException, ParseException, URISyntaxException {
		File mapJsonFile = visualizeMap(questionDir);
		visualizePolicies(questionDir, mapJsonFile);
	}

	public File visualizeMap(File questionDir)
			throws IOException, ParseException, URISyntaxException, MapTopologyException {
		FilenameFilter missionFileFilter = (dir, name) -> name.toLowerCase().matches("mission[0-9]+.json");
		// There is only 1 missionX.json file in each question dir
		File missionJsonFile = questionDir.listFiles(missionFileFilter)[0];
		JSONObject missionJsonObj = FileIOUtils.readJSONObjectFromFile(missionJsonFile);
		String mapFilename = (String) missionJsonObj.get("map-file");
		File mapJsonFile = FileIOUtils.getMapFile(MissionJSONGenerator.class, mapFilename);

		// Render map of the mission at /output/question-missionX/
		mMapRenderer.render(mapJsonFile, questionDir);

		return mapJsonFile;
	}

	public void visualizePolicies(File questionDir, File mapJsonFile)
			throws MapTopologyException, IOException, ParseException {
		// There can be 1 or more [policyName].json files in each question dir
		FilenameFilter policyFileFilter = (dir, name) -> name.toLowerCase().matches(".*policy[0-9]*.json");

		// Render all policies at /output/question-missionX/
		for (File policyJsonFile : questionDir.listFiles(policyFileFilter)) {
			mPolicyRenderer.render(policyJsonFile, mapJsonFile, questionDir, null);
		}

		// Visualize all policies in sub-directories recursively
		// Assume that all policies are of the same map
		FileFilter subDirFileFilter = File::isDirectory;
		for (File subDir : questionDir.listFiles(subDirFileFilter)) {
			visualizePolicies(subDir, mapJsonFile);
		}
	}
}
