package mobilerobot.policyviz;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.dsm.exceptions.NodeIDNotFoundException;
import guru.nidi.graphviz.model.MutableGraph;
import mobilerobot.utilities.FileIOUtils;

public class PolicyRenderer {

	private PolicyJSONToGraphViz mPolicyToGraph;

	public PolicyRenderer(File mapJsonFile, double meterPerInch, double scalingFactor)
			throws MapTopologyException, IOException, ParseException {
		GraphVizRenderer graphRenderer = new GraphVizRenderer(meterPerInch, scalingFactor);
		mPolicyToGraph = new PolicyJSONToGraphViz(mapJsonFile, graphRenderer);
	}

	public void render(File policyJsonFile) throws NodeIDNotFoundException, IOException, ParseException {
		MutableGraph policyGraph = mPolicyToGraph.convertPolicyJsonToGraph(policyJsonFile);
		String outputName = FilenameUtils.removeExtension(policyJsonFile.getName());
		GraphVizRenderer.drawGraph(policyGraph, outputName);
	}

	public static void main(String[] args)
			throws URISyntaxException, MapTopologyException, IOException, ParseException {
		String mapJsonFilename = args[0];
		File mapJsonFile = FileIOUtils.getFile(PolicyRenderer.class, FileIOUtils.MAPS_RESOURCE_PATH, mapJsonFilename);
		File policiesDir;
		if (args.length > 1) {
			String policiesPath = args[1];
			policiesDir = new File(policiesPath);
		} else {
			policiesDir = FileIOUtils.getPoliciesResourceDir(PolicyRenderer.class);
		}

		File[] policyJsonFiles = policiesDir.listFiles();
		PolicyRenderer policyRenderer = new PolicyRenderer(mapJsonFile, GraphVizRenderer.METER_PER_INCH,
				GraphVizRenderer.SCALING_FACTOR);

		for (File policyJsonFile : policyJsonFiles) {
			policyRenderer.render(policyJsonFile);
		}
	}
}
