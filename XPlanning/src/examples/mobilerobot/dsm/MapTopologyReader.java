package examples.mobilerobot.dsm;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.ConnectionNotFoundException;
import examples.mobilerobot.dsm.exceptions.LocationNodeNotFoundException;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;

public class MapTopologyReader {

	private JSONParser mParser = new JSONParser();
	private Set<? extends INodeAttributeParser<? extends INodeAttribute>> mNodeAttributeParsers;
	private Set<? extends IEdgeAttributeParser<? extends IEdgeAttribute>> mEdgeAttributeParsers;

	public MapTopologyReader(Set<? extends INodeAttributeParser<? extends INodeAttribute>> nodeAttributeParsers,
			Set<? extends IEdgeAttributeParser<? extends IEdgeAttribute>> edgeAttributeParsers) {
		mNodeAttributeParsers = nodeAttributeParsers;
		mEdgeAttributeParsers = edgeAttributeParsers;
	}

	public MapTopology readMapTopology(String mapJsonFilename)
			throws IOException, ParseException, MapTopologyException {
		FileReader reader = new FileReader(mapJsonFilename);
		Object object = mParser.parse(reader);

		JSONObject jsonObject = (JSONObject) object;

		// MPR: Meter-to-Pixel ratio
		int mpr = (int) jsonObject.get("mpr");

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

		// Add attributes to edges in the map
		parseAndPutEdgeAttributes(jsonObject, map);
		return map;
	}

	private LocationNode parseLocationNode(JSONObject nodeObject) {
		String nodeID = (String) nodeObject.get("node-id");
		JSONObject coordsObject = (JSONObject) nodeObject.get("coords");
		double xCoord = (double) coordsObject.get("x");
		double yCoord = (double) coordsObject.get("y");
		LocationNode locNode = new LocationNode(nodeID, xCoord, yCoord);
		for (INodeAttributeParser<? extends INodeAttribute> parser : mNodeAttributeParsers) {
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

	private void parseAndPutEdgeAttributes(JSONObject jsonObject, MapTopology map)
			throws LocationNodeNotFoundException, ConnectionNotFoundException {
		for (IEdgeAttributeParser<? extends IEdgeAttribute> parser : mEdgeAttributeParsers) {
			// Attributes: Array of edges
			JSONArray edgeArray = (JSONArray) jsonObject.get(parser.getAttributeName());
			for (Object obj : edgeArray) {
				JSONObject edgeObject = (JSONObject) obj;
				parseAndPutEdgeAttribute(parser, edgeObject, map);
			}
		}
	}

	private <E extends IEdgeAttribute> void parseAndPutEdgeAttribute(IEdgeAttributeParser<E> parser,
			JSONObject edgeObject, MapTopology map) throws LocationNodeNotFoundException, ConnectionNotFoundException {
		E value = parser.parseAttribute(edgeObject);
		String fromNodeID = (String) edgeObject.get("from-id");
		String toNodeID = (String) edgeObject.get("to-id");
		LocationNode fromNode = map.lookUpLocationNode(fromNodeID);
		LocationNode toNode = map.lookUpLocationNode(toNodeID);
		Connection connection = map.getConnection(fromNode, toNode);
		connection.putConnectionAttribute(parser.getAttributeName(), value);
	}
}
