package mobilerobot.policyviz;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import guru.nidi.graphviz.engine.Engine;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import mobilerobot.utilities.FileIOUtils;
import mobilerobot.utilities.GraphVizUtils;

public class GraphVizRenderer {

	static final double METER_PER_INCH = 10;
	static final double SCALING_FACTOR = 4;

	private static final double NODE_WIDTH = 0.6;
	private static final int NODE_BORDER_PENWIDTH = 2;

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

	/**
	 * Set node style for a non-start, non-goal node. The style does not include shape, but includes: fixedsize=true,
	 * width=NODE_WIDTH.
	 * 
	 * @param node
	 *            : Non-start, non-goal node
	 */
	public void setNodeStyle(MutableNode node) {
		setCommonNodeStyle(node);
	}

	/**
	 * Set node style for a start node. The style does not include shape, but includes: peripheries=2, and the common
	 * node style.
	 * 
	 * @param startNode
	 *            : Start node
	 */
	public void setStartNodeStyle(MutableNode startNode) {
		startNode.add("peripheries", 2);
		setCommonNodeStyle(startNode);
		GraphVizUtils.addUniqueNodeXLabel(startNode, "Start");
	}

	/**
	 * Set node style for a goal node. The style does not include shape, but includes: peripheries=2, and the common
	 * node style.
	 * 
	 * @param goalNode
	 *            : Goal node
	 */
	public void setGoalNodeStyle(MutableNode goalNode) {
		goalNode.add("peripheries", 2);
		setCommonNodeStyle(goalNode);
		GraphVizUtils.addUniqueNodeXLabel(goalNode, "Goal");
	}

	private void setCommonNodeStyle(MutableNode node) {
		node.add("fixedsize", "true");
		node.add("width", NODE_WIDTH);
		node.add("penwidth", NODE_BORDER_PENWIDTH);
	}

	public static void drawGraph(MutableGraph graph, File outDir, String outSubDirname, String outputName)
			throws IOException {
		File outputPNGFile = outSubDirname == null ? FileIOUtils.createOutFile(outDir, outputName + ".png")
				: FileIOUtils.createOutFile(outDir, outSubDirname, outputName + ".png");
		Graphviz.fromGraph(graph).engine(Engine.NEATO).render(Format.PNG).toFile(outputPNGFile);
	}

	public static void main(String[] args)
			throws URISyntaxException, IOException, ParseException, MapTopologyException {
		String option = args[0]; // option: "map" or "policy"
		File mapJsonFile;
		File policyJsonFile;

		if (option.equals("map")) {
			String mapJsonFilename = args[1]; // a single [mapName].json filename
			mapJsonFile = FileIOUtils.getMapFile(GraphVizRenderer.class, mapJsonFilename);

			// Render a single map
			MapRenderer mapRenderer = new MapRenderer(METER_PER_INCH, SCALING_FACTOR);
			mapRenderer.render(mapJsonFile);
		} else if (option.equals("policy")) {
			String mapJsonFilename = args[1]; // a single [mapName].json
			String policyJsonFilename = args[2]; // a single [policyName].json
			String startID = args[3];
			String goalID = args[4];
			mapJsonFile = FileIOUtils.getMapFile(GraphVizRenderer.class, mapJsonFilename);
			policyJsonFile = FileIOUtils.getPolicyFile(GraphVizRenderer.class, policyJsonFilename);

			// Render a single policy
			PolicyRenderer policyRenderer = new PolicyRenderer(METER_PER_INCH, SCALING_FACTOR);
			policyRenderer.render(policyJsonFile, mapJsonFile, startID, goalID);
		} else {
			throw new IllegalArgumentException("Unknown option: " + option);
		}

	}

}
