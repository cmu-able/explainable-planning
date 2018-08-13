package examples.mobilerobot.dsm.parser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
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

public class MapTopologyReader {

	private JSONParser mParser = new JSONParser();
	private Set<INodeAttributeParser<? extends INodeAttribute>> mNodeAttributeParsers;
	private Set<IEdgeAttributeParser<? extends IEdgeAttribute>> mEdgeAttributeParsers;

	public MapTopologyReader(Set<INodeAttributeParser<? extends INodeAttribute>> nodeAttributeParsers,
			Set<IEdgeAttributeParser<? extends IEdgeAttribute>> edgeAttributeParsers) {
		mNodeAttributeParsers = nodeAttributeParsers;
		mEdgeAttributeParsers = edgeAttributeParsers;
	}

	public MapTopology readMapTopology(File mapJsonFile) throws IOException, ParseException, MapTopologyException {
		FileReader reader = new FileReader(mapJsonFile);
		Object object = mParser.parse(reader);

		JSONObject jsonObject = (JSONObject) object;

		// MPR: Meter-to-Pixel ratio
		int mpr = JSONSimpleParserUtils.parseInt(jsonObject, "mpr");

		// Map: Array of nodes
		JSONArray nodeArray = (JSONArray) jsonObject.get("map");

		MapTopology map = new MapTopology();

		for (Object obj : nodeArray) {
			JSONObject nodeObject = (JSONObject) obj;
			LocationNode locNode = parseLocationNode(nodeObject);
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
					map.connect(locNode, neighborNode, distance);
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

	private LocationNode parseLocationNode(JSONObject nodeObject) {
		String nodeID = (String) nodeObject.get("node-id");
		JSONObject coordsObject = (JSONObject) nodeObject.get("coords");
		double xCoord = JSONSimpleParserUtils.parseDouble(coordsObject, "x");
		double yCoord = JSONSimpleParserUtils.parseDouble(coordsObject, "y");
		LocationNode locNode = new LocationNode(nodeID, xCoord, yCoord);
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

	private double calculateDistance(LocationNode srcNode, LocationNode destNode, double meterToPixelRatio) {
		double srcX = srcNode.getNodeXCoordinate();
		double srcY = srcNode.getNodeYCoordinate();
		double destX = destNode.getNodeXCoordinate();
		double destY = destNode.getNodeYCoordinate();
		double pixelDistance = Math.sqrt(Math.pow(destX - srcX, 2) + Math.pow(destY - srcY, 2));
		return pixelDistance / meterToPixelRatio;
	}

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

	private void parseEdgeAttributes(JSONObject jsonObject, MapTopology map)
			throws NodeIDNotFoundException, LocationNodeNotFoundException, ConnectionNotFoundException {
		for (IEdgeAttributeParser<? extends IEdgeAttribute> parser : mEdgeAttributeParsers) {
			// Edge attributes: Array of edges
			String attributeKey = parser.getJSONObjectKey();
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
