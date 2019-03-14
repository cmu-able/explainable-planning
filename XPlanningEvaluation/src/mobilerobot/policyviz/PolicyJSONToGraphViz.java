package mobilerobot.policyviz;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Factory.to;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

public class PolicyJSONToGraphViz {

	private JSONParser mJsonParser = new JSONParser();

	public MutableGraph convertPolicyJsonToGraph(File policyJsonFile) throws IOException, ParseException {
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

	private MutableNode parseNodeLink(JSONObject decisionJsonObj) {
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

}
