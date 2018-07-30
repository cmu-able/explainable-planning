package uiconnector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import language.mdp.StateVarTuple;
import language.policy.Decision;
import language.policy.Policy;
import language.qfactors.IAction;
import language.qfactors.IStateVarBoolean;
import language.qfactors.IStateVarDouble;
import language.qfactors.IStateVarInt;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;

public class PolicyWriter {

	private String mPolicyJsonDir;

	public PolicyWriter(String policyJsonDir) {
		mPolicyJsonDir = policyJsonDir;
	}

	public File writePolicy(Policy policy, String policyJsonFilename) throws IOException {
		JSONArray policyArray = new JSONArray();

		for (Decision decision : policy) {
			JSONObject stateJsonObj = writeState(decision.getState());
			JSONObject actionJsonObj = writeAction(decision.getAction());
			JSONObject decisionJsonObj = new JSONObject();
			decisionJsonObj.put("state", stateJsonObj);
			decisionJsonObj.put("action", actionJsonObj);
			policyArray.add(decisionJsonObj);
		}

		File policyJsonFile = new File(mPolicyJsonDir, policyJsonFilename);
		try (FileWriter writer = new FileWriter(policyJsonFile)) {
			writer.write(policyArray.toJSONString());
			writer.flush();
		}

		return policyJsonFile;
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
