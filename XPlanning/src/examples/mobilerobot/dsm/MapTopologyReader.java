package examples.mobilerobot.dsm;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MapTopologyReader {

	private JSONParser mParser = new JSONParser();

	public MapTopology readMapTopology(String mapJsonFilename) throws IOException, ParseException {
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
			LocationNode locNode = parseNodeObject(nodeObject);
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

		return map;
	}

	private LocationNode parseNodeObject(JSONObject nodeObject) {
		String nodeID = (String) nodeObject.get("node-id");
		JSONObject coordsObject = (JSONObject) nodeObject.get("coords");
		double xCoord = (double) coordsObject.get("x");
		double yCoord = (double) coordsObject.get("y");
		return new LocationNode(nodeID, xCoord, yCoord);
	}

	private double calculateDistance(LocationNode srcNode, LocationNode destNode, double meterToPixelRatio) {
		double srcX = srcNode.getNodeXCoordinate();
		double srcY = srcNode.getNodeYCoordinate();
		double destX = destNode.getNodeXCoordinate();
		double destY = destNode.getNodeYCoordinate();
		double pixelDistance = Math.sqrt(Math.pow(destX - srcX, 2) + Math.pow(destY - srcY, 2));
		return pixelDistance / meterToPixelRatio;
	}
}
