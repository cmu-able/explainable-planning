package mobilerobot.policyviz;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import guru.nidi.graphviz.model.MutableGraph;
import mobilerobot.utilities.FileIOUtils;

public class PolicyRenderer {

	private PolicyJSONToGraphViz mPolicyToGraph;

	public PolicyRenderer(double meterPerInch, double scalingFactor) {
		GraphVizRenderer graphRenderer = new GraphVizRenderer(meterPerInch, scalingFactor);
		mPolicyToGraph = new PolicyJSONToGraphViz(graphRenderer);
	}

	public void render(File policyJsonFile, File mapJsonFile) throws IOException, ParseException, MapTopologyException {
		MutableGraph policyGraph = mPolicyToGraph.convertPolicyJsonToGraph(policyJsonFile, mapJsonFile);
		String outputName = FilenameUtils.removeExtension(policyJsonFile.getName());
		GraphVizRenderer.drawGraph(policyGraph, outputName);
	}

	public static void main(String[] args)
			throws URISyntaxException, MapTopologyException, IOException, ParseException {
		String mapJsonFilename = args[0];
		File mapJsonFile = FileIOUtils.getMapFile(PolicyRenderer.class, mapJsonFilename);
		File policiesDir;
		if (args.length > 1) {
			String policiesPath = args[1];
			policiesDir = new File(policiesPath);
		} else {
			policiesDir = FileIOUtils.getPoliciesResourceDir(PolicyRenderer.class);
		}

		File[] policyJsonFiles = policiesDir.listFiles();
		PolicyRenderer policyRenderer = new PolicyRenderer(GraphVizRenderer.METER_PER_INCH,
				GraphVizRenderer.SCALING_FACTOR);

		for (File policyJsonFile : policyJsonFiles) {
			policyRenderer.render(policyJsonFile, mapJsonFile);
		}
	}
}
