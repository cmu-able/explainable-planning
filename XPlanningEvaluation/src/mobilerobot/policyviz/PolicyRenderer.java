package mobilerobot.policyviz;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import guru.nidi.graphviz.model.MutableGraph;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.xplanning.XPlanningRunner;

public class PolicyRenderer {

	private PolicyJSONToGraphViz mPolicyToGraph;

	public PolicyRenderer() {
		this(GraphVizRenderer.METER_PER_INCH, GraphVizRenderer.SCALING_FACTOR);
	}

	public PolicyRenderer(double meterPerInch, double scalingFactor) {
		GraphVizRenderer graphRenderer = new GraphVizRenderer(meterPerInch, scalingFactor);
		mPolicyToGraph = new PolicyJSONToGraphViz(graphRenderer, false);
	}

	/**
	 * 
	 * @param policiesDirOrFile
	 *            : Structure of policiesDir is /policies/ -> /mission[i]/ -> [policyName].json
	 * @param missionsRootDir
	 *            : Structure of missionsRootDir is /missions/ -> mission[i].json
	 * @throws IOException
	 * @throws ParseException
	 * @throws URISyntaxException
	 * @throws MapTopologyException
	 */
	public void renderAll(File policiesDirOrFile, File missionsRootDir)
			throws IOException, ParseException, URISyntaxException, MapTopologyException {
		if (!policiesDirOrFile.isDirectory()) {
			Path policyPath = policiesDirOrFile.toPath();
			int nameCount = policyPath.getNameCount();
			String missionName = policyPath.getName(nameCount - 2).toString();
			String missionFilename = missionName + ".json";

			File mapJsonFile = getMapJsonFile(missionFilename, missionsRootDir);
			String startID = getStringValueFromMission("start-id", missionFilename, missionsRootDir);
			String goalID = getStringValueFromMission("goal-id", missionFilename, missionsRootDir);
			render(policiesDirOrFile, mapJsonFile, startID, goalID, FileIOUtils.getOutputDir(), missionName);
		} else {
			for (File policiesSubDirOrFile : policiesDirOrFile.listFiles()) {
				renderAll(policiesSubDirOrFile, missionsRootDir);
			}
		}
	}

	public void render(File policyJsonFile, File mapJsonFile, String startID, String goalID)
			throws MapTopologyException, IOException, ParseException {
		render(policyJsonFile, mapJsonFile, startID, goalID, FileIOUtils.getOutputDir(), null);
	}

	public void render(File policyJsonFile, File mapJsonFile, String startID, String goalID, File outDir,
			String outSubDirname) throws IOException, ParseException, MapTopologyException {
		MutableGraph policyGraph = mPolicyToGraph.convertPolicyJsonToGraph(policyJsonFile, mapJsonFile, true, startID,
				goalID);
		String outputName = FilenameUtils.removeExtension(policyJsonFile.getName());
		GraphVizRenderer.drawGraph(policyGraph, outDir, outSubDirname, outputName);
	}

	private File getMapJsonFile(String missionJsonFilename, File missionsJsonRootDir)
			throws IOException, ParseException, URISyntaxException {
		String mapJsonFilename = getStringValueFromMission("map-file", missionJsonFilename, missionsJsonRootDir);
		return FileIOUtils.getMapFile(getClass(), mapJsonFilename);
	}

	private String getStringValueFromMission(String key, String missionJsonFilename, File missionsJsonRootDir)
			throws IOException, ParseException {
		File missionJsonFile = searchFile(missionJsonFilename, missionsJsonRootDir);
		JSONObject missionJsonObj = FileIOUtils.readJSONObjectFromFile(missionJsonFile);
		return (String) missionJsonObj.get(key);
	}

	private File searchFile(String filename, File dirOrFile) {
		if (!dirOrFile.isDirectory() && dirOrFile.getName().equals(filename)) {
			return dirOrFile;
		} else if (dirOrFile.isDirectory()) {
			for (File subDirOrFile : dirOrFile.listFiles()) {
				File file = searchFile(filename, subDirOrFile);
				if (file != null) {
					return file;
				}
			}
		}
		return null;
	}

	public static void main(String[] args)
			throws URISyntaxException, MapTopologyException, IOException, ParseException {
		PolicyRenderer policyRenderer = new PolicyRenderer();

		if (args.length > 1) {
			String policyJsonPath = args[0]; // path to arbitrary [policyName].json
			String mapJsonPath = args[1]; // path to arbitrary [mapName].json
			String startID = args[2];
			String goalID = args[3];

			File policyJsonFile = new File(policyJsonPath);
			File mapJsonFile = new File(mapJsonPath);

			// Render a single [policyName].json
			policyRenderer.render(policyJsonFile, mapJsonFile, startID, goalID);
		} else {
			File policiesDir = FileIOUtils.getPoliciesResourceDir(PolicyRenderer.class);
			File missionsRootDir = FileIOUtils.getMissionsResourceDir(XPlanningRunner.class);

			// Render all policies in policiesDir
			policyRenderer.renderAll(policiesDir, missionsRootDir);
		}

	}
}
