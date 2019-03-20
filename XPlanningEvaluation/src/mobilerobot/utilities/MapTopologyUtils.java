package mobilerobot.utilities;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.IEdgeAttribute;
import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.LocationNodeNotFoundException;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.dsm.parser.AreaParser;
import examples.mobilerobot.dsm.parser.IEdgeAttributeParser;
import examples.mobilerobot.dsm.parser.INodeAttributeParser;
import examples.mobilerobot.dsm.parser.MapTopologyReader;
import examples.mobilerobot.dsm.parser.OcclusionParser;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Occlusion;

public class MapTopologyUtils {

	private MapTopologyUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static MapTopology parseMapTopology(File mapJsonFile, boolean useAttrParsers)
			throws MapTopologyException, IOException, ParseException {
		// Area and Occlusion attribute parsers
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

		MapTopologyReader mapReader;
		if (useAttrParsers) {
			mapReader = new MapTopologyReader(nodeAttributeParsers, edgeAttributeParsers);
		} else {
			mapReader = new MapTopologyReader(new HashSet<>(), new HashSet<>());
		}
		return mapReader.readMapTopology(mapJsonFile, defaultNodeAttributes, defaultEdgeAttributes);
	}

	public static JSONObject convertMapTopologyToJSONObject(MapTopology mapTopology)
			throws LocationNodeNotFoundException {
		JSONObject mapJSONObj = new JSONObject();
		mapJSONObj.put("mur", 10); // use default MUR value = 10

		JSONArray nodesJSONArray = new JSONArray();

		for (LocationNode locNode : mapTopology) {
			JSONObject nodeJSONObj = convertLocationNodeToJSONObject(locNode, mapTopology);
			nodesJSONArray.add(nodeJSONObj);
		}
		mapJSONObj.put("map", nodesJSONArray);
		return mapJSONObj;
	}

	private static JSONObject convertLocationNodeToJSONObject(LocationNode locNode, MapTopology mapTopology)
			throws LocationNodeNotFoundException {
		JSONObject nodeJSONObj = new JSONObject();
		nodeJSONObj.put("node-id", locNode.getNodeID());

		JSONObject coordsJSONObj = new JSONObject();
		coordsJSONObj.put("x", locNode.getNodeXCoordinate());
		coordsJSONObj.put("y", locNode.getNodeYCoordinate());
		nodeJSONObj.put("coords", coordsJSONObj);

		JSONArray connJSONArray = new JSONArray();
		for (Connection connection : mapTopology.getConnections(locNode)) {
			LocationNode otherLocNode = connection.getOtherNode(locNode);
			connJSONArray.add(otherLocNode.getNodeID());
		}
		nodeJSONObj.put("connected-to", connJSONArray);
		return nodeJSONObj;
	}
}
