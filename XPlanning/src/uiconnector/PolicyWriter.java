package uiconnector;

import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import factors.IAction;
import mdp.StateVarTuple;
import policy.Decision;
import policy.Policy;

public class PolicyWriter {

	private FileWriter mWriter;

	public PolicyWriter(String policyJsonFilename) throws IOException {
		mWriter = new FileWriter(policyJsonFilename);
	}

	public void writePolicy(Policy policy) {
		JSONArray policyArray = new JSONArray();

		for (Decision decision : policy) {
			JSONObject stateJsonObj = writeState(decision.getState());
			JSONObject actionJsonObj = writeAction(decision.getAction());
			JSONObject decisionJsonObj = new JSONObject();
			decisionJsonObj.put("state", stateJsonObj);
			decisionJsonObj.put("action", actionJsonObj);
			policyArray.add(decisionJsonObj);
		}
	}

	private JSONObject writeState(StateVarTuple stateVarTuple) {
		JSONObject stateJsonObj = new JSONObject();
		// TODO
		return stateJsonObj;
	}

	private JSONObject writeAction(IAction action) {
		JSONObject actionJsonObj = new JSONObject();
		// TODO
		return actionJsonObj;
	}
}
