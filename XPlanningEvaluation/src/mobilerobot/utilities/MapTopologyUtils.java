package mobilerobot.utilities;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.IEdgeAttribute;
import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.ConnectionAttributeNotFoundException;
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

	public static LocationNode[] getLocationNodeArray(MapTopology mapTopology) {
		LocationNode[] locNodes = new LocationNode[mapTopology.getNumNodes()];
		int i = 0;
		for (LocationNode locNode : mapTopology) {
			locNodes[i] = locNode;
			i++;
		}
		return locNodes;
	}

	public static JSONObject convertMapTopologyToJSONObject(MapTopology mapTopology) throws MapTopologyException {
		JSONObject mapJSONObj = new JSONObject();

		// mur
		mapJSONObj.put("mur", 10); // use default meter-unit ratio value = 10

		// map
		JSONArray nodesJSONArray = new JSONArray();
		for (LocationNode locNode : mapTopology) {
			JSONObject nodeJSONObj = convertLocationNodeToJSONObject(locNode, mapTopology);
			nodesJSONArray.add(nodeJSONObj);
		}
		mapJSONObj.put("map", nodesJSONArray);

		// obstacles
		ConnectionAttributeFilter<Occlusion> filter = new ConnectionAttributeFilter<>("occlusion",
				Occlusion.PARTIALLY_OCCLUDED, Occlusion.BLOCKED);
		JSONArray obstacleJSONArray = convertConnectionAttributesToJSONArray(mapTopology.connectionIterator(),
				"occlusion", filter);
		mapJSONObj.put("obstacles", obstacleJSONArray);

		return mapJSONObj;
	}

	private static JSONObject convertLocationNodeToJSONObject(LocationNode locNode, MapTopology mapTopology)
			throws MapTopologyException {
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

		Area area = locNode.getNodeAttribute(Area.class, "area");
		nodeJSONObj.put("area", area.toString());

		return nodeJSONObj;
	}

	private static <E extends IEdgeAttribute> JSONArray convertConnectionAttributesToJSONArray(
			Iterator<Connection> iter, String attributeName, ConnectionAttributeFilter<E> filter)
			throws ConnectionAttributeNotFoundException {
		JSONArray connAttrJSONArray = new JSONArray();
		while (iter.hasNext()) {
			Connection connection = iter.next();
			if (filter.filter(connection)) {
				JSONObject connAttrJSONObj = convertConnectionAttributeToJSONObject(connection, attributeName);
				connAttrJSONArray.add(connAttrJSONObj);
			}
		}
		return connAttrJSONArray;
	}

	private static JSONObject convertConnectionAttributeToJSONObject(Connection connection, String attributeName)
			throws ConnectionAttributeNotFoundException {
		JSONObject connAttrJSONObj = new JSONObject();
		connAttrJSONObj.put("from-id", connection.getNodeA().getNodeID());
		connAttrJSONObj.put("to-id", connection.getNodeB().getNodeID());

		IEdgeAttribute connAttribute = connection.getGenericConnectionAttribute(attributeName);
		connAttrJSONObj.put(attributeName, connAttribute.toString());

		return connAttrJSONObj;
	}

	private static class ConnectionAttributeFilter<E extends IEdgeAttribute> {

		private String mAttributeName;

		private Set<E> mAttributeFilter;

		ConnectionAttributeFilter(String attributeName, E... attributes) {
			mAttributeName = attributeName;
			mAttributeFilter = Stream.of(attributes).collect(Collectors.toSet());
		}

		public boolean filter(Connection connection) throws ConnectionAttributeNotFoundException {
			IEdgeAttribute connAttr = connection.getGenericConnectionAttribute(mAttributeName);
			return mAttributeFilter.contains(connAttr);
		}
	}
}
