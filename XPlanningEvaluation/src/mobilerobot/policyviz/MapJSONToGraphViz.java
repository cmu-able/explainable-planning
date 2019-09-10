package mobilerobot.policyviz;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Factory.to;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Occlusion;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import mobilerobot.utilities.MapTopologyUtils;
import uiconnector.JSONSimpleParserUtils;

public class MapJSONToGraphViz {

	// Area types
	private static final String FILLCOLOR_KEY = "fillcolor"; // Use "fillcolor" attribute to have outline around each node
	private static final String PUBLIC_AREA_COLOR = "green";
	private static final String SEMI_PRIVATE_AREA_COLOR = "yellow";
	private static final String PRIVATE_AREA_COLOR = "red";

	// Obstacle density
	private static final String OBSTACLE_BLOCKS = "<td bgcolor='darkgoldenrod3' width='15' height='30'> </td><td bgcolor='darkgoldenrod4' width='15' height='30'> </td>";
	private static final String SPARSE_OBSTACLE_LABEL = "<table border='0'><tr>" + OBSTACLE_BLOCKS + "</tr></table>";
	private static final String DENSE_OBSTACLE_LABEL = "<table border='0'><tr>" + OBSTACLE_BLOCKS + OBSTACLE_BLOCKS
			+ "</tr></table>";

	private static final int NODE_FONT_SIZE = 28;
	private static final int CONNECTION_LINE_WIDTH = 2;

	private MapTopology mMapTopology;
	private double mMeterUnitRatio;
	private GraphVizRenderer mGraphRenderer;

	public MapJSONToGraphViz(File mapJsonFile, GraphVizRenderer graphRenderer)
			throws MapTopologyException, IOException, ParseException {
		mMapTopology = MapTopologyUtils.parseMapTopology(mapJsonFile, true);
		JSONParser jsonParser = new JSONParser();
		JSONObject mapJsonObj = (JSONObject) jsonParser.parse(new FileReader(mapJsonFile));
		mMeterUnitRatio = JSONSimpleParserUtils.parseDouble(mapJsonObj, "mur");
		mGraphRenderer = graphRenderer;
	}

	public MutableGraph convertMapJsonToGraph() throws MapTopologyException {
		return convertMapJsonToGraph(null, null);
	}

	public MutableGraph convertMapJsonToGraph(String startID, String goalID) throws MapTopologyException {
		MutableGraph mapGraph = mutGraph("map").setDirected(false);
		Set<LocationNode> visitedLocNodes = new HashSet<>();

		for (LocationNode locNode : mMapTopology) {
			MutableNode nodeLink = parseNodeLink(locNode, visitedLocNodes, startID, goalID);
			mapGraph.add(nodeLink);
		}

		mapGraph.nodeAttrs().add(Font.size(NODE_FONT_SIZE));
		return mapGraph;
	}

	private MutableNode parseNodeLink(LocationNode locNode, Set<LocationNode> visitedLocNodes, String startID,
			String goalID) throws MapTopologyException {
		String nodeID = locNode.getNodeID();
		MutableNode node = mutNode(nodeID);

		mGraphRenderer.setRelativeNodePosition(node, locNode.getNodeXCoordinate(), locNode.getNodeYCoordinate(),
				mMeterUnitRatio);

		if (startID != null && nodeID.equals(startID)) {
			mGraphRenderer.setStartNodeStyle(node);
		} else if (goalID != null && nodeID.equals(goalID)) {
			mGraphRenderer.setGoalNodeStyle(node);
		} else {
			mGraphRenderer.setNodeStyle(node);
		}

		Area area = locNode.getNodeAttribute(Area.class, "area");
		node.add(Style.FILLED);

		if (area == Area.PUBLIC) {
			node.add(FILLCOLOR_KEY, PUBLIC_AREA_COLOR);
			node.add(Shape.CIRCLE);
		} else if (area == Area.SEMI_PRIVATE) {
			node.add(FILLCOLOR_KEY, SEMI_PRIVATE_AREA_COLOR);
			node.add("shape", "square");
		} else if (area == Area.PRIVATE) {
			node.add(FILLCOLOR_KEY, PRIVATE_AREA_COLOR);
			node.add(Shape.PENTAGON);
		}

		parseLinks(node, locNode, visitedLocNodes);

		// Mark this node as visited: all of its neighbors are added to the graph
		visitedLocNodes.add(locNode);
		return node;
	}

	private void parseLinks(MutableNode graphNode, LocationNode locNode, Set<LocationNode> visitedLocNodes)
			throws MapTopologyException {
		for (Connection connection : mMapTopology.getConnections(locNode)) {
			LocationNode neighborLocNode = connection.getOtherNode(locNode);

			if (visitedLocNodes.contains(neighborLocNode)) {
				// This neighbor node has already been visited
				// The link from the neighbor node to this node was already created
				continue;
			}

			Occlusion occlusion = connection.getConnectionAttribute(Occlusion.class, "occlusion");
			MutableNode neighborNode = mutNode(neighborLocNode.getNodeID());
			Link neighborLink;
			if (occlusion == Occlusion.CLEAR) {
				neighborLink = to(neighborNode);
			} else if (occlusion == Occlusion.PARTIALLY_OCCLUDED) {
				neighborLink = to(neighborNode).with(Label.html(SPARSE_OBSTACLE_LABEL));
			} else if (occlusion == Occlusion.OCCLUDED) {
				neighborLink = to(neighborNode).with(Label.html(DENSE_OBSTACLE_LABEL));
			} else {
				throw new IllegalArgumentException("Unknown occlusion value: " + occlusion);
			}

			neighborLink = neighborLink.with(Style.lineWidth(CONNECTION_LINE_WIDTH));
			graphNode.addLink(neighborLink);
		}
	}
}
