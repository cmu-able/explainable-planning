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
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import mobilerobot.utilities.PolicyJSONParserUtils;

public class PolicyJSONToGraphViz {

	private JSONParser mJsonParser = new JSONParser();
	private GraphVizRenderer mGraphRenderer;

	public PolicyJSONToGraphViz(GraphVizRenderer graphRenderer) {
		mGraphRenderer = graphRenderer;
	}

	public MutableGraph convertPolicyJsonToGraph(File policyJsonFile, File mapJsonFile, boolean withMap)
			throws IOException, ParseException, MapTopologyException {
		FileReader policyReader = new FileReader(policyJsonFile);
		JSONObject policyJsonObj = (JSONObject) mJsonParser.parse(policyReader);
		JSONArray policyJsonArray = (JSONArray) policyJsonObj.get("policy");

		MutableGraph policyGraph;
		INodeLinkFormatter nodeLinkFormatter;

		if (withMap) {
			MapJSONToGraphViz mapToGraph = new MapJSONToGraphViz(mapJsonFile, mGraphRenderer);
			policyGraph = mapToGraph.convertMapJsonToGraph();
			//nodeLinkFormatter = (srcNode, destNode) -> to(destNode).with(Style.lineWidth(5), Color.BLUE);
			nodeLinkFormatter = new INodeLinkFormatter() {

				@Override
				public Link createMoveToLink(MutableNode srcNode, MutableNode destNode) throws NodeIDNotFoundException {
					return to(destNode).with(Style.lineWidth(5), Color.BLUE);
				}

				@Override
				public String createSetSpeedLabel(double targetSpeed, boolean rBumped) {
					// TODO Auto-generated method stub
					return null;
				}
			};
		} else {
			MapTopologyReader mapReader = new MapTopologyReader(new HashSet<>(), new HashSet<>());
			MapTopology map = mapReader.readMapTopology(mapJsonFile);
			JSONObject mapJsonObj = (JSONObject) mJsonParser.parse(new FileReader(mapJsonFile));
			double mur = JSONSimpleParserUtils.parseDouble(mapJsonObj, "mur");
			policyGraph = mutGraph("policy").setDirected(true);
			nodeLinkFormatter = new INodeLinkFormatter() {

				@Override
				public Link createMoveToLink(MutableNode srcNode, MutableNode destNode) throws NodeIDNotFoundException {
					setNodePosition(srcNode, map, mur);
					setNodePosition(destNode, map, mur);
					mGraphRenderer.setNodeStyle(srcNode);
					mGraphRenderer.setNodeStyle(destNode);
					return to(destNode);
				}

				@Override
				public String createSetSpeedLabel(double targetSpeed, boolean rBumped) {
					return "setSpeed(" + targetSpeed + ")";
				}
			};
		}

		for (Object obj : policyJsonArray) {
			JSONObject decisionJsonObj = (JSONObject) obj;
			MutableNode nodeLink = parseNodeLink(decisionJsonObj, nodeLinkFormatter);
			policyGraph.add(nodeLink);
		}

		return policyGraph;
	}

	private MutableNode parseNodeLink(JSONObject decisionJsonObj, INodeLinkFormatter nodeLinkFormatter)
			throws NodeIDNotFoundException {
		JSONObject stateJsonObj = (JSONObject) decisionJsonObj.get("state");
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get("action");

		String rLoc = (String) stateJsonObj.get("rLoc");
		String actionType = (String) actionJsonObj.get("type");
		JSONArray actionParamJsonArray = (JSONArray) actionJsonObj.get("params");

		if (actionType.equals("moveTo")) {
			String destLoc = (String) actionParamJsonArray.get(0);
			MutableNode srcNode = mutNode(rLoc);
			MutableNode destNode = mutNode(destLoc);
			Link moveToLink = nodeLinkFormatter.createMoveToLink(srcNode, destNode);
			srcNode.addLink(moveToLink);
			return srcNode;
		} else if (actionType.equals("setSpeed")) {
			Double targetSpeed = Double.parseDouble((String) actionParamJsonArray.get(0));
			String actionLabel = "setSpeed(" + targetSpeed + ")";
			MutableNode rLocNode = mutNode(rLoc);
			rLocNode.add("xlabel", actionLabel);
			return rLocNode;
		}

		throw new IllegalArgumentException("Unknown action type: " + actionType);
	}

	private void setNodePosition(MutableNode node, MapTopology map, double mur) throws NodeIDNotFoundException {
		String nodeID = node.name().toString();
		LocationNode locNode = map.lookUpLocationNode(nodeID);
		double xCoord = locNode.getNodeXCoordinate();
		double yCoord = locNode.getNodeYCoordinate();
		mGraphRenderer.setRelativeNodePosition(node, xCoord, yCoord, mur);
	}

	private String createSetSpeedLabel(JSONObject decisionJsonObj, JSONArray policyJsonArray) {
		String rLoc = PolicyJSONParserUtils.parseStringVar("rLoc", decisionJsonObj);
		double rSpeed = PolicyJSONParserUtils.parseDoubleVar("rSpeed", decisionJsonObj);
		boolean rBumped = PolicyJSONParserUtils.parseBooleanVar("rBumped", decisionJsonObj);
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get("action");
		double targetSpeed = PolicyJSONParserUtils.parseDoubleActionParameter(0, decisionJsonObj);
		StringBuilder builder = new StringBuilder();
		builder.append("setSpeed(" + targetSpeed + ")");

		for (Object obj : policyJsonArray) {
			JSONObject decisionJsonObjP = (JSONObject) obj;
			String rLocP = PolicyJSONParserUtils.parseStringVar("rLoc", decisionJsonObjP);
			double rSpeedP = PolicyJSONParserUtils.parseDoubleVar("rSpeed", decisionJsonObjP);
			boolean rBumpedP = PolicyJSONParserUtils.parseBooleanVar("rBumped", decisionJsonObjP);
			JSONObject actionJsonObjP = (JSONObject) decisionJsonObjP.get("action");

			if (rLoc.equals(rLocP) && rSpeed == rSpeedP && rBumped != rBumpedP
					&& !actionJsonObj.equals(actionJsonObjP)) {
				builder.append(" | " + (rBumped ? "bumped" : "!bumped"));
				break;
			}
		}
		return builder.toString();
	}

	private interface INodeLinkFormatter {
		Link createMoveToLink(MutableNode srcNode, MutableNode destNode) throws NodeIDNotFoundException;

		String createSetSpeedLabel(double targetSpeed, boolean rBumped);
	}
}