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
import examples.mobilerobot.dsm.parser.JSONSimpleParserUtils;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Occlusion;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import mobilerobot.utilities.MapTopologyUtils;

public class MapJSONToGraphViz {

	private static final Color PUBLIC_AREA_COLOR = Color.GREEN;
	private static final Color SEMI_PRIVATE_AREA_COLOR = Color.YELLOW;
	private static final Color PRIVATE_AREA_COLOR = Color.RED;
	private static final int OCCLUSION_FONT_SIZE = 24;
	private static final int NODE_FONT_SIZE = 20;

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
		MutableGraph mapGraph = mutGraph("map").setDirected(false);
		Set<LocationNode> visitedLocNodes = new HashSet<>();

		for (LocationNode locNode : mMapTopology) {
			MutableNode nodeLink = parseNodeLink(locNode, visitedLocNodes);
			mapGraph.add(nodeLink);
		}

		mapGraph.nodeAttrs().add(Font.size(NODE_FONT_SIZE));
		return mapGraph;
	}

	private MutableNode parseNodeLink(LocationNode locNode, Set<LocationNode> visitedLocNodes)
			throws MapTopologyException {
		MutableNode node = mutNode(locNode.getNodeID());

		mGraphRenderer.setRelativeNodePosition(node, locNode.getNodeXCoordinate(), locNode.getNodeYCoordinate(),
				mMeterUnitRatio);
		mGraphRenderer.setNodeStyle(node);

		Area area = locNode.getNodeAttribute(Area.class, "area");
		if (area == Area.PUBLIC) {
			node.add(PUBLIC_AREA_COLOR, Style.FILLED);
		} else if (area == Area.SEMI_PRIVATE) {
			node.add(SEMI_PRIVATE_AREA_COLOR, Style.FILLED);
		} else if (area == Area.PRIVATE) {
			node.add(PRIVATE_AREA_COLOR, Style.FILLED);
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
				neighborLink = to(neighborNode).with(Label.of("PO")).with(Font.size(OCCLUSION_FONT_SIZE));
			} else if (occlusion == Occlusion.OCCLUDED) {
				neighborLink = to(neighborNode).with(Label.of("O")).with(Font.size(OCCLUSION_FONT_SIZE));
			} else {
				throw new IllegalArgumentException("Unknown occlusion value: " + occlusion);
			}
			graphNode.addLink(neighborLink);
		}
	}
}
