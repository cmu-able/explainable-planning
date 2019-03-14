package mobilerobot.policyviz;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;

public class GraphVizRenderer {

	private static final String MAPS_RESOURCE_PATH = "maps";
	private static final String POLICIES_RESOURCE_PATH = "policies";
	private static final String OUTPUT_PATH = "output";
	private static final double METER_PER_INCH = 10;

	public static void drawGraph(MutableGraph graph, String outputName) throws IOException {
		File outputPNGFile = new File(OUTPUT_PATH, outputName + ".png");
		Graphviz.fromGraph(graph).engine(Engine.NEATO).render(Format.PNG).toFile(outputPNGFile);
	}

	public static void main(String[] args) throws URISyntaxException, IOException, ParseException {
		String option = args[0];
		String jsonFilename = args[1];
		URL resourceFolderURL;
		if (option.equals("map")) {
			resourceFolderURL = PolicyJSONToGraphViz.class.getResource(MAPS_RESOURCE_PATH);
		} else if (option.equals("policy")) {
			resourceFolderURL = PolicyJSONToGraphViz.class.getResource(POLICIES_RESOURCE_PATH);
		} else {
			throw new IllegalArgumentException("Unknown option: " + option);
		}

		File resourceFolder = new File(resourceFolderURL.toURI());
		File jsonFile = new File(resourceFolder, jsonFilename);
		if (!jsonFile.exists()) {
			throw new FileNotFoundException("File not found: " + jsonFile);
		}

		MutableGraph graph = null;
		if (option.equals("map")) {
			MapJSONToGraphViz mapViz = new MapJSONToGraphViz(METER_PER_INCH);
			graph = mapViz.convertMapJsonToGraph(jsonFile);
		} else if (option.equals("policy")) {
			PolicyJSONToGraphViz policyViz = new PolicyJSONToGraphViz();
			graph = policyViz.convertPolicyJsonToGraph(jsonFile);
		}

		String outputName = FilenameUtils.removeExtension(jsonFile.getName());
		drawGraph(graph, outputName);
	}

}
