package mobilerobot.policyviz;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import guru.nidi.graphviz.model.MutableGraph;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.xplanning.XPlanningRunner;

public class PolicyRenderer {

	private JSONParser mJsonParser = new JSONParser();
	private PolicyJSONToGraphViz mPolicyToGraph;

	public PolicyRenderer(double meterPerInch, double scalingFactor) {
		GraphVizRenderer graphRenderer = new GraphVizRenderer(meterPerInch, scalingFactor);
		mPolicyToGraph = new PolicyJSONToGraphViz(graphRenderer);
	}

	public void renderAll(File policiesDirOrFile)
			throws IOException, ParseException, URISyntaxException, MapTopologyException {
		if (!policiesDirOrFile.isDirectory()) {
			Path policyPath = policiesDirOrFile.toPath();
			int nameCount = policyPath.getNameCount();
			String missionName = policyPath.getName(nameCount - 2).toString();
			File mapJsonFile = getMapJsonFile(missionName + ".json",
					FileIOUtils.getMissionsResourceDir(XPlanningRunner.class));
			render(policiesDirOrFile, mapJsonFile, missionName);
		} else {
			for (File policiesSubDirOrFile : policiesDirOrFile.listFiles()) {
				renderAll(policiesSubDirOrFile);
			}
		}
	}

	public void render(File policyJsonFile, File mapJsonFile) throws MapTopologyException, IOException, ParseException {
		render(policyJsonFile, mapJsonFile, null);
	}

	public void render(File policyJsonFile, File mapJsonFile, String outputSubDirname)
			throws IOException, ParseException, MapTopologyException {
		MutableGraph policyGraph = mPolicyToGraph.convertPolicyJsonToGraph(policyJsonFile, mapJsonFile, true);
		String outputName = FilenameUtils.removeExtension(policyJsonFile.getName());
		GraphVizRenderer.drawGraph(policyGraph, outputSubDirname, outputName);
	}

	private File getMapJsonFile(String missionJsonFilename, File missionsJsonRootDir)
			throws IOException, ParseException, URISyntaxException {
		File missionJsonFile = searchFile(missionJsonFilename, missionsJsonRootDir);
		JSONObject missionJsonObj = (JSONObject) mJsonParser.parse(new FileReader(missionJsonFile));
		String mapJsonFilename = (String) missionJsonObj.get("map-file");
		return FileIOUtils.getMapFile(getClass(), mapJsonFilename);
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
		File policiesDir;
		if (args.length > 1) {
			String policiesPath = args[1];
			policiesDir = new File(policiesPath);
		} else {
			policiesDir = FileIOUtils.getPoliciesResourceDir(PolicyRenderer.class);
		}

		PolicyRenderer policyRenderer = new PolicyRenderer(GraphVizRenderer.METER_PER_INCH,
				GraphVizRenderer.SCALING_FACTOR);

		policyRenderer.renderAll(policiesDir);
	}
}
