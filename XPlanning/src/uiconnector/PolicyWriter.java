package uiconnector;

import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import factors.IAction;
import factors.IStateVarBoolean;
import factors.IStateVarDouble;
import factors.IStateVarInt;
import factors.IStateVarValue;
import factors.StateVar;
import mdp.StateVarTuple;
import policy.Decision;
import policy.Policy;

public class PolicyWriter {

	public void writePolicy(Policy policy, String policyJsonFilename) throws IOException {
		JSONArray policyArray = new JSONArray();

		for (Decision decision : policy) {
			JSONObject stateJsonObj = writeState(decision.getState());
			JSONObject actionJsonObj = writeAction(decision.getAction());
			JSONObject decisionJsonObj = new JSONObject();
			decisionJsonObj.put("state", stateJsonObj);
			decisionJsonObj.put("action", actionJsonObj);
			policyArray.add(decisionJsonObj);
		}

		try (FileWriter writer = new FileWriter(policyJsonFilename)) {
			writer.write(policyArray.toJSONString());
			writer.flush();
		}
	}

	private JSONObject writeState(StateVarTuple stateVarTuple) {
		JSONObject stateJsonObj = new JSONObject();
		for (StateVar<IStateVarValue> stateVar : stateVarTuple) {
			String varName = stateVar.getName();
			IStateVarValue value = stateVar.getValue();
			if (value instanceof IStateVarBoolean) {
				IStateVarBoolean boolValue = (IStateVarBoolean) value;
				stateJsonObj.put(varName, boolValue.getValue());
			} else if (value instanceof IStateVarInt) {
				IStateVarInt intValue = (IStateVarInt) value;
				stateJsonObj.put(varName, intValue.getValue());
			} else if (value instanceof IStateVarDouble) {
				IStateVarDouble doubleValue = (IStateVarDouble) value;
				stateJsonObj.put(varName, doubleValue.getValue());
			} else {
				stateJsonObj.put(varName, value.toString());
			}
		}
		return stateJsonObj;
	}

	private JSONObject writeAction(IAction action) {
		JSONObject actionJsonObj = new JSONObject();
		actionJsonObj.put("type", action.getNamePrefix());
		JSONArray paramArray = new JSONArray();
		paramArray.addAll(action.getParameters());
		actionJsonObj.put("params", paramArray);
		return actionJsonObj;
	}
}
