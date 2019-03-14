package mobilerobot.policyviz;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Factory.to;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

public class MapJSONToGraphViz {

	private JSONParser mJsonParser = new JSONParser();

	public MutableGraph convertMapJsonToGraph(File mapJsonFile) throws IOException, ParseException {
		FileReader reader = new FileReader(mapJsonFile);
		Object object = mJsonParser.parse(reader);
		JSONObject mapJsonObj = (JSONObject) object;
		JSONArray nodeJsonArray = (JSONArray) mapJsonObj.get("map");
		JSONArray obstacleJsonArray = (JSONArray) mapJsonObj.get("obstacles");

		MutableGraph mapGraph = mutGraph("map").setDirected(false);
		Set<String> visitedNodeIDs = new HashSet<>();

		for (Object obj : nodeJsonArray) {
			JSONObject nodeJsonObj = (JSONObject) obj;
			MutableNode nodeLink = parseNodeLink(nodeJsonObj, obstacleJsonArray, visitedNodeIDs);
			mapGraph.add(nodeLink);
		}

		return mapGraph;
	}

	private MutableNode parseNodeLink(JSONObject nodeJsonObj, JSONArray obstacleJsonArray, Set<String> visitedNodeIDs) {
		String nodeID = (String) nodeJsonObj.get("node-id");
		JSONArray connectedToJsonArray = (JSONArray) nodeJsonObj.get("connected-to");
		String area = (String) nodeJsonObj.get("area");

		MutableNode node = mutNode(nodeID);

		if (area.equals("PUBLIC")) {
			node.add(Color.GREEN, Style.FILLED);
		} else if (area.equals("SEMI_PRIVATE")) {
			node.add(Color.YELLOW, Style.FILLED);
		} else if (area.equals("PRIVATE")) {
			node.add(Color.RED, Style.FILLED);
		} else {
			throw new IllegalArgumentException("Unknown area type: " + area);
		}

		for (Object obj : connectedToJsonArray) {
			String neighborNodeID = (String) obj;

			if (visitedNodeIDs.contains(neighborNodeID)) {
				// This neighbor node has already been visited
				// The link from the neighbor node to this node was already created
				continue;
			}

			String occlusion = getOcclusion(nodeID, neighborNodeID, obstacleJsonArray);
			MutableNode neighborNode = mutNode(neighborNodeID);
			if (occlusion.equals("CLEAR")) {
				node.addLink(neighborNode);
			} else {
				node.addLink(to(neighborNode).with(Label.of(occlusion)));
			}
		}

		// Mark this node as visited: all of its neighbors are added to the graph
		visitedNodeIDs.add(nodeID);
		return node;
	}

	private String getOcclusion(String nodeID, String neighborNodeID, JSONArray obstacleJsonArray) {
		for (Object obj : obstacleJsonArray) {
			JSONObject obstacleJsonObj = (JSONObject) obj;
			String fromID = (String) obstacleJsonObj.get("from-id");
			String toID = (String) obstacleJsonObj.get("to-id");
			String occlusion = (String) obstacleJsonObj.get("occlusion");

			if ((nodeID.equals(fromID)) && (neighborNodeID.equals(toID))
					|| (nodeID.equals(toID) && (neighborNodeID.equals(fromID)))) {
				return occlusion;
			}
		}
		return "CLEAR";
	}

}
