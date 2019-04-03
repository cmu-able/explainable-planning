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
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import mobilerobot.utilities.PolicyJSONParserUtils;

public class PolicyJSONToGraphViz {

	private static final String R_LOC = "rLoc";
	private static final String R_SPEED = "rSpeed";
	private static final String R_BUMPED = "rBumped";

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
			nodeLinkFormatter = new INodeLinkFormatter() {

				@Override
				public Link createMoveToLink(JSONObject decisionJsonObj) throws NodeIDNotFoundException {
					String destLoc = PolicyJSONParserUtils.parseStringActionParameter(0, decisionJsonObj);
					MutableNode destNode = mutNode(destLoc);
					Link moveToLink = to(destNode).with(Style.lineWidth(5));
					moveToLink = addrSpeedLinkColor(moveToLink, decisionJsonObj);
					moveToLink = addNecessaryrBumpedConditionLabel(moveToLink, decisionJsonObj, policyJsonArray);
					return moveToLink;
				}

				@Override
				public String createSetSpeedLabel(JSONObject decisionJsonObj) {
					return PolicyJSONToGraphViz.this.createSetSpeedLabel(decisionJsonObj, policyJsonArray);
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
				public Link createMoveToLink(JSONObject decisionJsonObj) throws NodeIDNotFoundException {
					String rLoc = PolicyJSONParserUtils.parseStringVar(R_LOC, decisionJsonObj);
					String destLoc = PolicyJSONParserUtils.parseStringActionParameter(0, decisionJsonObj);
					MutableNode srcNode = mutNode(rLoc);
					MutableNode destNode = mutNode(destLoc);
					setNodePosition(srcNode, map, mur);
					setNodePosition(destNode, map, mur);
					mGraphRenderer.setNodeStyle(srcNode);
					mGraphRenderer.setNodeStyle(destNode);
					Link moveToLink = to(destNode);
					moveToLink = addrSpeedLinkColor(moveToLink, decisionJsonObj);
					moveToLink = addNecessaryrBumpedConditionLabel(moveToLink, decisionJsonObj, policyJsonArray);
					return moveToLink;
				}

				@Override
				public String createSetSpeedLabel(JSONObject decisionJsonObj) {
					return PolicyJSONToGraphViz.this.createSetSpeedLabel(decisionJsonObj, policyJsonArray);
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

	private Link addrSpeedLinkColor(Link moveToLink, JSONObject decisionJsonObj) {
		double rSpeed = PolicyJSONParserUtils.parseDoubleVar(R_SPEED, decisionJsonObj);
		if (rSpeed == 0.35) {
			return moveToLink.with(Color.LIGHTBLUE);
		} else if (rSpeed == 0.68) {
			return moveToLink.with(Color.BLUE);
		} else {
			throw new IllegalArgumentException("Unknown rSpeed value: " + rSpeed);
		}
	}

	private Link addNecessaryrBumpedConditionLabel(Link moveToLink, JSONObject decisionJsonObj,
			JSONArray policyJsonArray) {
		String rBumpedCond = createrBumpedConditionLabel(decisionJsonObj, policyJsonArray);
		return rBumpedCond == null ? moveToLink : moveToLink.with(Label.of(rBumpedCond));
	}

	private String createSetSpeedLabel(JSONObject decisionJsonObj, JSONArray policyJsonArray) {
		double targetSpeed = PolicyJSONParserUtils.parseDoubleActionParameter(0, decisionJsonObj);
		String actionLabel = "setSpeed(" + targetSpeed + ")";
		String rBumpedCond = createrBumpedConditionLabel(decisionJsonObj, policyJsonArray);
		return rBumpedCond == null ? actionLabel : actionLabel + " | " + rBumpedCond;
	}

	private String createrBumpedConditionLabel(JSONObject decisionJsonObj, JSONArray policyJsonArray) {
		// Conditions on rLoc and rSpeed are explicitly visualized for moveTo actions.
		// Condition on rLoc is explicitly visualized for setSpeed actions, and condition on rSpeed is 
		// implicit in the actions.
		// Thus, the only condition that must be represented is on rBumped.

		String rLoc = PolicyJSONParserUtils.parseStringVar(R_LOC, decisionJsonObj);
		double rSpeed = PolicyJSONParserUtils.parseDoubleVar(R_SPEED, decisionJsonObj);
		boolean rBumped = PolicyJSONParserUtils.parseBooleanVar(R_BUMPED, decisionJsonObj);
		JSONObject actionJsonObj = (JSONObject) decisionJsonObj.get("action");

		for (Object obj : policyJsonArray) {
			JSONObject decisionJsonObjP = (JSONObject) obj;
			String rLocP = PolicyJSONParserUtils.parseStringVar(R_LOC, decisionJsonObjP);
			double rSpeedP = PolicyJSONParserUtils.parseDoubleVar(R_SPEED, decisionJsonObjP);
			boolean rBumpedP = PolicyJSONParserUtils.parseBooleanVar(R_BUMPED, decisionJsonObjP);
			JSONObject actionJsonObjP = (JSONObject) decisionJsonObjP.get("action");

			// If there are multiple states at the decision's (location, speed) with different rBumped values and different actions,
			// then explicitly label the rBumped condition of this decision
			if (rLoc.equals(rLocP) && rSpeed == rSpeedP && rBumped != rBumpedP
					&& !actionJsonObj.equals(actionJsonObjP)) {
				return rBumped ? "bumped" : "!bumped";
			}
		}

		// No need to explicitly label the rBumped condition of this decision
		return null;
	}

	private MutableNode parseNodeLink(JSONObject decisionJsonObj, INodeLinkFormatter nodeLinkFormatter)
			throws NodeIDNotFoundException {
		String rLoc = PolicyJSONParserUtils.parseStringVar(R_LOC, decisionJsonObj);
		String actionType = PolicyJSONParserUtils.parseActionType(decisionJsonObj);
		MutableNode rLocNode = mutNode(rLoc);

		if (actionType.equals("moveTo")) {
			Link moveToLink = nodeLinkFormatter.createMoveToLink(decisionJsonObj);
			rLocNode.addLink(moveToLink);
			return rLocNode;
		} else if (actionType.equals("setSpeed")) {
			String setSpeedLabel = nodeLinkFormatter.createSetSpeedLabel(decisionJsonObj);
			rLocNode.add("xlabel", setSpeedLabel);
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

	private interface INodeLinkFormatter {
		Link createMoveToLink(JSONObject decisionJsonObj) throws NodeIDNotFoundException;

		String createSetSpeedLabel(JSONObject decisionJsonObj);
	}
}