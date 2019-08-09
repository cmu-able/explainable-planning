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
import examples.mobilerobot.dsm.parser.MapTopologyReader;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import mobilerobot.utilities.GraphVizUtils;
import uiconnector.JSONSimpleParserUtils;
import uiconnector.PolicyJSONParserUtils;

public class PolicyJSONToGraphViz {

	private static final String R_LOC = "rLoc";
	private static final String R_SPEED = "rSpeed";
	private static final String R_BUMPED = "rBumped";
	private static final int MOVE_TO_LINE_WIDTH = 12;
	private static final Color HALF_SPEED_COLOR = Color.LIGHTBLUE;
	private static final Color FULL_SPEED_COLOR = Color.BLUE;

	private JSONParser mJsonParser = new JSONParser();
	private GraphVizRenderer mGraphRenderer;
	private boolean mShowSetSpeedLabel;

	public PolicyJSONToGraphViz(GraphVizRenderer graphRenderer, boolean showSetSpeedLabel) {
		mGraphRenderer = graphRenderer;
		mShowSetSpeedLabel = showSetSpeedLabel;
	}

	public MutableGraph convertPolicyJsonToGraph(File policyJsonFile, File mapJsonFile, boolean withMap, String startID,
			String goalID) throws IOException, ParseException, MapTopologyException {
		FileReader policyReader = new FileReader(policyJsonFile);
		JSONObject policyJsonObj = (JSONObject) mJsonParser.parse(policyReader);
		JSONArray policyJsonArray = (JSONArray) policyJsonObj.get("policy");

		MutableGraph policyGraph;
		IMoveToLinkFormatter moveToLinkFormatter;
		ISetSpeedLabelFormatter setSpeedLabelFormatter = decisionJsonObj -> {
			double targetSpeed = PolicyJSONParserUtils.parseDoubleActionParameter(0, decisionJsonObj);
			String actionLabel = "setSpeed(" + targetSpeed + ")";

			if (PolicyJSONParserUtils.containsVar(R_BUMPED, decisionJsonObj)) {
				String rBumpedCond = createrBumpedConditionLabel(decisionJsonObj, policyJsonArray);
				return rBumpedCond == null ? actionLabel : actionLabel + " | " + rBumpedCond;
			}

			return actionLabel;
		};

		if (withMap) {
			MapJSONToGraphViz mapToGraph = new MapJSONToGraphViz(mapJsonFile, mGraphRenderer);
			policyGraph = mapToGraph.convertMapJsonToGraph(startID, goalID);
			moveToLinkFormatter = decisionJsonObj -> createMoveToLinkWithMap(decisionJsonObj, policyJsonArray);
		} else {
			MapTopologyReader mapReader = new MapTopologyReader(new HashSet<>(), new HashSet<>());
			MapTopology map = mapReader.readMapTopology(mapJsonFile);
			JSONObject mapJsonObj = (JSONObject) mJsonParser.parse(new FileReader(mapJsonFile));
			double mur = JSONSimpleParserUtils.parseDouble(mapJsonObj, "mur");
			policyGraph = mutGraph("policy").setDirected(true);
			moveToLinkFormatter = decisionJsonObj -> createMoveToLinkWithoutMap(decisionJsonObj, policyJsonArray, map,
					mur);
		}

		for (Object obj : policyJsonArray) {
			JSONObject decisionJsonObj = (JSONObject) obj;
			String actionType = PolicyJSONParserUtils.parseActionType(decisionJsonObj);

			if (actionType.equals("moveTo")) {
				addMoveToLinkToNode(policyGraph, decisionJsonObj, moveToLinkFormatter);
			} else if (actionType.equals("setSpeed") && mShowSetSpeedLabel) {
				addSetSpeedLabelToNode(policyGraph, decisionJsonObj, setSpeedLabelFormatter);
			}
		}

		return policyGraph;
	}

	private Link createMoveToLinkWithMap(JSONObject decisionJsonObj, JSONArray policyJsonArray) {
		String destLoc = PolicyJSONParserUtils.parseStringActionParameter(0, decisionJsonObj);
		MutableNode destNode = mutNode(destLoc);
		Link moveToLink = to(destNode).with(Style.lineWidth(MOVE_TO_LINE_WIDTH));
		addrSpeedLinkColor(moveToLink, decisionJsonObj);

		if (PolicyJSONParserUtils.containsVar(R_BUMPED, decisionJsonObj)) {
			addNecessaryrBumpedConditionLabel(moveToLink, decisionJsonObj, policyJsonArray);
		}

		return moveToLink;
	}

	private Link createMoveToLinkWithoutMap(JSONObject decisionJsonObj, JSONArray policyJsonArray, MapTopology map,
			double mur) throws NodeIDNotFoundException {
		String rLoc = PolicyJSONParserUtils.parseStringVar(R_LOC, decisionJsonObj);
		String destLoc = PolicyJSONParserUtils.parseStringActionParameter(0, decisionJsonObj);
		MutableNode srcNode = mutNode(rLoc);
		MutableNode destNode = mutNode(destLoc);
		setNodePosition(srcNode, map, mur);
		setNodePosition(destNode, map, mur);
		mGraphRenderer.setNodeStyle(srcNode);
		mGraphRenderer.setNodeStyle(destNode);
		Link moveToLink = to(destNode);
		addrSpeedLinkColor(moveToLink, decisionJsonObj);

		if (PolicyJSONParserUtils.containsVar(R_BUMPED, decisionJsonObj)) {
			addNecessaryrBumpedConditionLabel(moveToLink, decisionJsonObj, policyJsonArray);
		}

		return moveToLink;
	}

	private void addrSpeedLinkColor(Link moveToLink, JSONObject decisionJsonObj) {
		double rSpeed = PolicyJSONParserUtils.parseDoubleVar(R_SPEED, decisionJsonObj);
		if (rSpeed == 0.35) {
			moveToLink.add(HALF_SPEED_COLOR);
		} else if (rSpeed == 0.68) {
			moveToLink.add(FULL_SPEED_COLOR);
		} else {
			throw new IllegalArgumentException("Unknown rSpeed value: " + rSpeed);
		}
	}

	private void addNecessaryrBumpedConditionLabel(Link moveToLink, JSONObject decisionJsonObj,
			JSONArray policyJsonArray) {
		String rBumpedCond = createrBumpedConditionLabel(decisionJsonObj, policyJsonArray);
		if (rBumpedCond != null) {
			moveToLink.add(Label.of(rBumpedCond));
		}
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

	private void addMoveToLinkToNode(MutableGraph policyGraph, JSONObject decisionJsonObj,
			IMoveToLinkFormatter formatter) throws NodeIDNotFoundException {
		String rLoc = PolicyJSONParserUtils.parseStringVar(R_LOC, decisionJsonObj);
		MutableNode rLocNode = GraphVizUtils.lookUpNode(policyGraph, rLoc);
		Link moveToLink = formatter.createMoveToLink(decisionJsonObj);
		GraphVizUtils.addUniqueMoveToLink(rLocNode, moveToLink);
	}

	private void addSetSpeedLabelToNode(MutableGraph policyGraph, JSONObject decisionJsonObj,
			ISetSpeedLabelFormatter formatter) {
		String rLoc = PolicyJSONParserUtils.parseStringVar(R_LOC, decisionJsonObj);
		MutableNode rLocNode = GraphVizUtils.lookUpNode(policyGraph, rLoc);
		String setSpeedLabel = formatter.createSetSpeedLabel(decisionJsonObj);
		GraphVizUtils.addUniqueNodeXLabel(rLocNode, setSpeedLabel);
	}

	private void setNodePosition(MutableNode node, MapTopology map, double mur) throws NodeIDNotFoundException {
		String nodeID = node.name().toString();
		LocationNode locNode = map.lookUpLocationNode(nodeID);
		double xCoord = locNode.getNodeXCoordinate();
		double yCoord = locNode.getNodeYCoordinate();
		mGraphRenderer.setRelativeNodePosition(node, xCoord, yCoord, mur);
	}

	private interface IMoveToLinkFormatter {
		Link createMoveToLink(JSONObject decisionJsonObj) throws NodeIDNotFoundException;
	}

	private interface ISetSpeedLabelFormatter {
		String createSetSpeedLabel(JSONObject decisionJsonObj);
	}
}