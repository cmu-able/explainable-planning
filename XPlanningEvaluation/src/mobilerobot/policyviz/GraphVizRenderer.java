package mobilerobot.policyviz;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import mobilerobot.utilities.FileIOUtils;

public class GraphVizRenderer {

	static final double METER_PER_INCH = 10;
	static final double SCALING_FACTOR = 4;

	private double mMeterPerInch;
	private double mScalingFactor;

	public GraphVizRenderer(double meterPerInch, double scalingFactor) {
		mMeterPerInch = meterPerInch;
		mScalingFactor = scalingFactor;
	}

	public void setRelativeNodePosition(MutableNode node, double xCoord, double yCoord, double mur) {
		double adjustedXCoord = mScalingFactor * xCoord * mur / mMeterPerInch;
		double adjustedYCoord = mScalingFactor * yCoord * mur / mMeterPerInch;
		String nodePos = adjustedXCoord + "," + adjustedYCoord + "!";
		node.add("pos", nodePos);
	}

	public void setNodeStyle(MutableNode node) {
		node.add(Shape.CIRCLE);
		node.add("width", "0.1");
	}

	public static void drawGraph(MutableGraph graph, String outputName) throws IOException {
		File outputPNGFile = FileIOUtils.createOutputFile(outputName + ".png");
		Graphviz.fromGraph(graph).engine(Engine.NEATO).render(Format.PNG).toFile(outputPNGFile);
	}

	public static void main(String[] args)
			throws URISyntaxException, IOException, ParseException, MapTopologyException {
		String option = args[0];
		File mapJsonFile;
		File policyJsonFile;

		if (option.equals("map")) {
			String mapJsonFilename = args[1];
			mapJsonFile = FileIOUtils.getMapFile(GraphVizRenderer.class, mapJsonFilename);

			// Render map
			MapRenderer mapRenderer = new MapRenderer(METER_PER_INCH, SCALING_FACTOR);
			mapRenderer.render(mapJsonFile);
		} else if (option.equals("policy")) {
			String mapJsonFilename = args[1];
			String policyJsonFilename = args[2];
			mapJsonFile = FileIOUtils.getMapFile(GraphVizRenderer.class, mapJsonFilename);
			policyJsonFile = FileIOUtils.getPolicyFile(GraphVizRenderer.class, policyJsonFilename);

			// Render policy
			PolicyRenderer policyRenderer = new PolicyRenderer(mapJsonFile, METER_PER_INCH, SCALING_FACTOR);
			policyRenderer.render(policyJsonFile);
		} else {
			throw new IllegalArgumentException("Unknown option: " + option);
		}

	}

}
