package examples.mobilerobot.dsm.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.IEdgeAttribute;
import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.ConnectionNotFoundException;
import examples.mobilerobot.dsm.exceptions.LocationNodeNotFoundException;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.dsm.exceptions.NodeIDNotFoundException;
import uiconnector.JSONSimpleParserUtils;

public class MapTopologyReader {

	private JSONParser mParser = new JSONParser();
	private Set<INodeAttributeParser<? extends INodeAttribute>> mNodeAttributeParsers;
	private Set<IEdgeAttributeParser<? extends IEdgeAttribute>> mEdgeAttributeParsers;

	public MapTopologyReader(Set<INodeAttributeParser<? extends INodeAttribute>> nodeAttributeParsers,
			Set<IEdgeAttributeParser<? extends IEdgeAttribute>> edgeAttributeParsers) {
		mNodeAttributeParsers = nodeAttributeParsers;
		mEdgeAttributeParsers = edgeAttributeParsers;
	}

	public MapTopology readMapTopology(File mapJsonFile) throws MapTopologyException, IOException, ParseException {
		return readMapTopology(mapJsonFile, new HashMap<>(), new HashMap<>());
	}

	public MapTopology readMapTopology(File mapJsonFile, Map<String, INodeAttribute> defaultNodeAttributes,
			Map<String, IEdgeAttribute> defaultEdgeAttributes)
			throws IOException, ParseException, MapTopologyException {
		FileReader reader = new FileReader(mapJsonFile);
		Object object = mParser.parse(reader);

		JSONObject jsonObject = (JSONObject) object;

		// MUR: Meter-to-Unit ratio
		int mpr = JSONSimpleParserUtils.parseInt(jsonObject, "mur");

		// Map: Array of nodes
		JSONArray nodeArray = (JSONArray) jsonObject.get("map");

		MapTopology map = new MapTopology();

		for (Object obj : nodeArray) {
			JSONObject nodeObject = (JSONObject) obj;
			LocationNode locNode = parseLocationNode(nodeObject, defaultNodeAttributes);
			map.addLocationNode(locNode);
		}

		// Nodes that have been connected to all of its neighbors
		Set<LocationNode> visitedNodes = new HashSet<>();

		for (Object obj : nodeArray) {
			JSONObject nodeObject = (JSONObject) obj;
			String nodeID = (String) nodeObject.get("node-id");
			LocationNode locNode = map.lookUpLocationNode(nodeID);
			JSONArray neighborArray = (JSONArray) nodeObject.get("connected-to");

			for (Object innerObj : neighborArray) {
				String neighborID = (String) innerObj;
				LocationNode neighborNode = map.lookUpLocationNode(neighborID);

				if (!visitedNodes.contains(neighborNode)) {
					double distance = calculateDistance(locNode, neighborNode, mpr);
					map.connect(locNode, neighborNode, distance, defaultEdgeAttributes);
				}
			}

			visitedNodes.add(locNode);
		}

		// Parse and add additional attributes to nodes in the map
		parseAdditionalNodeAttributes(jsonObject, map);

		// Parse and add attributes to edges in the map
		parseEdgeAttributes(jsonObject, map);
		return map;
	}

	private LocationNode parseLocationNode(JSONObject nodeObject, Map<String, INodeAttribute> defaultNodeAttributes) {
		String nodeID = (String) nodeObject.get("node-id");
		JSONObject coordsObject = (JSONObject) nodeObject.get("coords");
		double xCoord = JSONSimpleParserUtils.parseDouble(coordsObject, "x");
		double yCoord = JSONSimpleParserUtils.parseDouble(coordsObject, "y");
		LocationNode locNode = new LocationNode(nodeID, xCoord, yCoord, defaultNodeAttributes);
		for (INodeAttributeParser<? extends INodeAttribute> parser : mNodeAttributeParsers) {

			if (!nodeObject.containsKey(parser.getJSONObjectKey())) {
				// This attribute is not specified in a node object, but it may be specified as a key-value pair in the
				// JSONObject
				continue;
			}

			INodeAttribute value = parser.parseAttribute(nodeObject);
			locNode.putNodeAttribute(parser.getAttributeName(), value);
		}
		return locNode;
	}

	private double calculateDistance(LocationNode srcNode, LocationNode destNode, double meterToUnitRatio) {
		double srcX = srcNode.getNodeXCoordinate();
		double srcY = srcNode.getNodeYCoordinate();
		double destX = destNode.getNodeXCoordinate();
		double destY = destNode.getNodeYCoordinate();
		double arbitraryUnitDistance = Math.sqrt(Math.pow(destX - srcX, 2) + Math.pow(destY - srcY, 2));
		return arbitraryUnitDistance * meterToUnitRatio;
	}

	/**
	 * Parse any additional node attributes from a given JSONObject. An additional node attribute is not specified in
	 * the node object, but is specified in a key-value pair in the JSONObject. Additional node attributes are for
	 * features of the map that are sparse (e.g., placement of lights).
	 * 
	 * The key is the plural name of the additional node attribute. The value is an array of objects of the form {
	 * "at-id" : [nodeID], [nodeAttributeName] : [nodeAttributeValue] }. Some nodes may be omitted from the value array;
	 * those nodes will have the default node-attribute value.
	 * 
	 * @param jsonObject
	 *            : JSONObject defining the map
	 * @param map
	 *            : MapTopology to set node-attribute values
	 * @throws NodeIDNotFoundException
	 */
	private void parseAdditionalNodeAttributes(JSONObject jsonObject, MapTopology map) throws NodeIDNotFoundException {
		for (INodeAttributeParser<? extends INodeAttribute> parser : mNodeAttributeParsers) {
			String attributeKey = parser.getJSONObjectKey();

			if (jsonObject.containsKey(attributeKey)) {
				// This attribute is specified as a key-value pair in the JSONObject

				// Additional node attributes: Array of objects with node ids
				JSONArray nodeAttributeArray = (JSONArray) jsonObject.get(attributeKey);
				for (Object obj : nodeAttributeArray) {
					JSONObject nodeAttributeObject = (JSONObject) obj;
					String nodeID = (String) nodeAttributeObject.get("at-id");
					INodeAttribute value = parser.parseAttribute(nodeAttributeObject);
					LocationNode node = map.lookUpLocationNode(nodeID);
					node.putNodeAttribute(parser.getAttributeName(), value);
				}
			}
		}
	}

	/**
	 * Parse edge attributes from a given JSONObject. An edge attribute is specified in a key-value pair in the
	 * JSONObject.
	 * 
	 * The key is the plural name of the edge attribute. The value is an array of objects of the form { "from-id" :
	 * [nodeID], "to-id" : [nodeID], [edgeAttributeName] : [edgeAttributeValue] }. Some edges may be omitted from the
	 * value array; those edges will have the default edge-attribute value.
	 * 
	 * @param jsonObject
	 *            : JSONObject defining the map
	 * @param map
	 *            : MapTopology to set edge-attribute values
	 * @throws NodeIDNotFoundException
	 * @throws LocationNodeNotFoundException
	 * @throws ConnectionNotFoundException
	 */
	private void parseEdgeAttributes(JSONObject jsonObject, MapTopology map)
			throws NodeIDNotFoundException, LocationNodeNotFoundException, ConnectionNotFoundException {
		for (IEdgeAttributeParser<? extends IEdgeAttribute> parser : mEdgeAttributeParsers) {
			// Edge attributes: Array of edges
			String attributeKey = parser.getJSONObjectKey();

			if (!jsonObject.containsKey(attributeKey)) {
				// This edge attribute is not specified in the map JSON
				continue;
			}

			JSONArray edgeArray = (JSONArray) jsonObject.get(attributeKey);
			for (Object obj : edgeArray) {
				JSONObject edgeObject = (JSONObject) obj;
				IEdgeAttribute value = parser.parseAttribute(edgeObject);
				String fromNodeID = (String) edgeObject.get("from-id");
				String toNodeID = (String) edgeObject.get("to-id");
				LocationNode fromNode = map.lookUpLocationNode(fromNodeID);
				LocationNode toNode = map.lookUpLocationNode(toNodeID);
				Connection connection = map.getConnection(fromNode, toNode);
				connection.putConnectionAttribute(parser.getAttributeName(), value);
			}
		}
	}
}
