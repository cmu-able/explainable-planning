package mobilerobot.policyviz;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Factory.to;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.IEdgeAttribute;
import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.dsm.parser.AreaParser;
import examples.mobilerobot.dsm.parser.IEdgeAttributeParser;
import examples.mobilerobot.dsm.parser.INodeAttributeParser;
import examples.mobilerobot.dsm.parser.JSONSimpleParserUtils;
import examples.mobilerobot.dsm.parser.MapTopologyReader;
import examples.mobilerobot.dsm.parser.OcclusionParser;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Occlusion;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

public class MapJSONToGraphViz {

	private MapTopology mMapTopology;
	private double mMeterUnitRatio;

	public MapJSONToGraphViz(File mapJsonFile) throws MapTopologyException, IOException, ParseException {
		mMapTopology = parseMapTopology(mapJsonFile);
		JSONParser jsonParser = new JSONParser();
		JSONObject mapJsonObj = (JSONObject) jsonParser.parse(new FileReader(mapJsonFile));
		mMeterUnitRatio = JSONSimpleParserUtils.parseDouble(mapJsonObj, "mur");
	}

	private MapTopology parseMapTopology(File mapJsonFile) throws MapTopologyException, IOException, ParseException {
		AreaParser areaParser = new AreaParser();
		OcclusionParser occlusionParser = new OcclusionParser();
		Set<INodeAttributeParser<? extends INodeAttribute>> nodeAttributeParsers = new HashSet<>();
		nodeAttributeParsers.add(areaParser);
		Set<IEdgeAttributeParser<? extends IEdgeAttribute>> edgeAttributeParsers = new HashSet<>();
		edgeAttributeParsers.add(occlusionParser);

		// Default node/edge attribute values
		Map<String, INodeAttribute> defaultNodeAttributes = new HashMap<>();
		Map<String, IEdgeAttribute> defaultEdgeAttributes = new HashMap<>();
		defaultNodeAttributes.put(areaParser.getAttributeName(), Area.PUBLIC);
		defaultEdgeAttributes.put(occlusionParser.getAttributeName(), Occlusion.CLEAR);

		MapTopologyReader reader = new MapTopologyReader(nodeAttributeParsers, edgeAttributeParsers);
		return reader.readMapTopology(mapJsonFile, defaultNodeAttributes, defaultEdgeAttributes);
	}

	public MutableGraph convertMapJsonToGraph() throws MapTopologyException {
		MutableGraph mapGraph = mutGraph("map").setDirected(false);
		Set<LocationNode> visitedLocNodes = new HashSet<>();

		for (LocationNode locNode : mMapTopology) {
			MutableNode nodeLink = parseNodeLink(locNode, visitedLocNodes);
			mapGraph.add(nodeLink);
		}

		return mapGraph;
	}

	private MutableNode parseNodeLink(LocationNode locNode, Set<LocationNode> visitedLocNodes)
			throws MapTopologyException {
		MutableNode node = mutNode(locNode.getNodeID());
		setNodeStyle(node);
		GraphVizRenderer.setRelativeNodePosition(node, locNode.getNodeXCoordinate(), locNode.getNodeYCoordinate(),
				mMeterUnitRatio);

		Area area = locNode.getNodeAttribute(Area.class, "area");
		if (area == Area.PUBLIC) {
			node.add(Color.GREEN, Style.FILLED);
		} else if (area == Area.SEMI_PRIVATE) {
			node.add(Color.YELLOW, Style.FILLED);
		} else if (area == Area.PRIVATE) {
			node.add(Color.RED, Style.FILLED);
		}

		for (Connection connection : mMapTopology.getConnections(locNode)) {
			LocationNode neighborLocNode = connection.getOtherNode(locNode);

			if (visitedLocNodes.contains(neighborLocNode)) {
				// This neighbor node has already been visited
				// The link from the neighbor node to this node was already created
				continue;
			}

			Occlusion occlusion = connection.getConnectionAttribute(Occlusion.class, "occlusion");
			MutableNode neighborNode = mutNode(neighborLocNode.getNodeID());
			if (occlusion == Occlusion.CLEAR) {
				node.addLink(neighborNode);
			} else {
				node.addLink(to(neighborNode).with(Label.of(occlusion.toString())));
			}
		}

		// Mark this node as visited: all of its neighbors are added to the graph
		visitedLocNodes.add(locNode);
		return node;
	}

	private void setNodeStyle(MutableNode node) {
		node.add(Shape.CIRCLE);
		node.add("width", "0.1");
	}
}
