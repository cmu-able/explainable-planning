package mobilerobot.policyviz;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

public class GraphVizRenderer {

	private static final String MAPS_RESOURCE_PATH = "maps";
	private static final String POLICIES_RESOURCE_PATH = "policies";
	private static final String OUTPUT_PATH = "output";
	private static final double METER_PER_INCH = 10;
	private static final double SCALING_FACTOR = 4;

	public static void drawGraph(MutableGraph graph, String outputName) throws IOException {
		File outputPNGFile = new File(OUTPUT_PATH, outputName + ".png");
		Graphviz.fromGraph(graph).engine(Engine.NEATO).render(Format.PNG).toFile(outputPNGFile);
	}

	public static void setRelativeNodePosition(MutableNode node, double xCoord, double yCoord, double mur) {
		double adjustedXCoord = SCALING_FACTOR * xCoord * mur / METER_PER_INCH;
		double adjustedYCoord = SCALING_FACTOR * yCoord * mur / METER_PER_INCH;
		String nodePos = adjustedXCoord + "," + adjustedYCoord + "!";
		node.add("pos", nodePos);
	}

	public static void setNodeStyle(MutableNode node) {
		node.add(Shape.CIRCLE);
		node.add("width", "0.1");
	}

	public static void main(String[] args)
			throws URISyntaxException, IOException, ParseException, MapTopologyException {
		String option = args[0];
		File mapJsonFile;
		File policyJsonFile;
		MutableGraph graph = null;

		if (option.equals("map")) {
			String mapJsonFilename = args[1];
			mapJsonFile = getFile(MAPS_RESOURCE_PATH, mapJsonFilename);
			MapJSONToGraphViz mapViz = new MapJSONToGraphViz(mapJsonFile);
			graph = mapViz.convertMapJsonToGraph();
		} else if (option.equals("policy")) {
			String mapJsonFilename = args[1];
			String policyJsonFilename = args[2];
			mapJsonFile = getFile(MAPS_RESOURCE_PATH, mapJsonFilename);
			policyJsonFile = getFile(POLICIES_RESOURCE_PATH, policyJsonFilename);
			PolicyJSONToGraphViz policyViz = new PolicyJSONToGraphViz(mapJsonFile);
			graph = policyViz.convertPolicyJsonToGraph(policyJsonFile);
		} else {
			throw new IllegalArgumentException("Unknown option: " + option);
		}

		String outputName = FilenameUtils.removeExtension(mapJsonFile.getName());
		drawGraph(graph, outputName);
	}

	private static File getFile(String resourcePath, String filename) throws URISyntaxException, FileNotFoundException {
		URL resourceFolderURL = GraphVizRenderer.class.getResource(resourcePath);
		File resourceFolder = new File(resourceFolderURL.toURI());
		File file = new File(resourceFolder, filename);
		if (!file.exists()) {
			throw new FileNotFoundException("File not found: " + file);
		}
		return file;
	}

}
