package mobilerobot.policyviz;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Factory.to;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.dsm.exceptions.NodeIDNotFoundException;
import examples.mobilerobot.dsm.parser.JSONSimpleParserUtils;
import examples.mobilerobot.dsm.parser.MapTopologyReader;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

public class PolicyJSONToGraphViz {

	private MapTopology mMapTopology;
	private double mMeterUnitRatio;
	private JSONParser mJsonParser = new JSONParser();
	private GraphVizRenderer mGraphRenderer;

	public PolicyJSONToGraphViz(File mapJsonFile, GraphVizRenderer graphRenderer)
			throws MapTopologyException, IOException, ParseException {
		MapTopologyReader reader = new MapTopologyReader(new HashSet<>(), new HashSet<>());
		mMapTopology = reader.readMapTopology(mapJsonFile);
		JSONObject mapJsonObj = (JSONObject) mJsonParser.parse(new FileReader(mapJsonFile));
		mMeterUnitRatio = JSONSimpleParserUtils.parseDouble(mapJsonObj, "mur");
		mGraphRenderer = graphRenderer;
	}

	public MutableGraph convertPolicyJsonToGraph(File policyJsonFile)
			throws IOException, ParseException, NodeIDNotFoundException {
		FileReader reader = new FileReader(policyJsonFile);
		Object object = mJsonParser.parse(reader);
		JSONArray policyJsonArray = (JSONArray) object;
		MutableGraph policyGraph = mutGraph("policy").setDirected(true);
		for (Object obj : policyJsonArray) {
			JSONObject decisionJsonObj = (JSONObject) obj;
			MutableNode nodeLink = parseNodeLink(decisionJsonObj);
			policyGraph.add(nodeLink);
		}
		return policyGraph;
	}

	private MutableNode parseNodeLink(JSONObject decisionJsonObj) throws NodeIDNotFoundException {
		JSONObject stateJsonObj = (JSONObject) decisionJsonObj.get("state");
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get("action");

		String rLoc = (String) stateJsonObj.get("rLoc");
		String actionType = (String) actionJsonObj.get("type");
		JSONArray actionParamJsonArray = (JSONArray) actionJsonObj.get("params");

		if (actionType.equals("moveTo")) {
			String destLoc = (String) actionParamJsonArray.get(0);
			MutableNode srcNode = mutNode(rLoc);
			MutableNode destNode = mutNode(destLoc);
			srcNode.addLink(destNode);

			setNodePosition(srcNode, rLoc);
			setNodePosition(destNode, destLoc);
			mGraphRenderer.setNodeStyle(srcNode);
			mGraphRenderer.setNodeStyle(destNode);

			return srcNode;
		} else if (actionType.equals("setSpeed")) {
			Double targetSpeed = Double.parseDouble((String) actionParamJsonArray.get(0));
			String actionLabel = "setSpeed(" + targetSpeed + ")";
			MutableNode rLocNode = mutNode(rLoc);
			MutableNode actionNode = mutNode(actionLabel + "@" + rLoc);
			actionNode.add(Shape.RECTANGLE);
			rLocNode.addLink(to(actionNode).with(Style.DOTTED));
			actionNode.addLink(to(rLocNode).with(Style.DOTTED));
			return rLocNode;
		}

		throw new IllegalArgumentException("Unknown action type: " + actionType);
	}

	private void setNodePosition(MutableNode node, String nodeID) throws NodeIDNotFoundException {
		LocationNode locNode = mMapTopology.lookUpLocationNode(nodeID);
		double xCoord = locNode.getNodeXCoordinate();
		double yCoord = locNode.getNodeYCoordinate();
		mGraphRenderer.setRelativeNodePosition(node, xCoord, yCoord, mMeterUnitRatio);
	}
}