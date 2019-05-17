package uiconnector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import language.domain.models.IAction;
import language.domain.models.IStateVarBoolean;
import language.domain.models.IStateVarDouble;
import language.domain.models.IStateVarInt;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.mdp.StateVarTuple;
import language.policy.Decision;
import language.policy.Policy;

public class PolicyWriter {

	private File mPolicyJsonDir;

	public PolicyWriter(File policyJsonDir) {
		mPolicyJsonDir = policyJsonDir;
		mPolicyJsonDir.mkdirs(); // only make directories when ones don't exist

	}

	public File writePolicy(Policy policy, String policyJsonFilename) throws IOException {
		JSONObject policyJsonObj = writePolicyJSONObject(policy);

		File policyJsonFile = new File(mPolicyJsonDir, policyJsonFilename);
		try (FileWriter writer = new FileWriter(policyJsonFile)) {
			writer.write(policyJsonObj.toJSONString());
			writer.flush();
		}

		return policyJsonFile;
	}

	public static JSONObject writePolicyJSONObject(Policy policy) {
		JSONArray policyJsonArray = new JSONArray();
		for (Decision decision : policy) {
			JSONObject stateJsonObj = writeState(decision.getState());
			JSONObject actionJsonObj = writeAction(decision.getAction());
			JSONObject decisionJsonObj = new JSONObject();
			decisionJsonObj.put("state", stateJsonObj);
			decisionJsonObj.put("action", actionJsonObj);
			policyJsonArray.add(decisionJsonObj);
		}

		JSONObject policyJsonObj = new JSONObject();
		policyJsonObj.put("policy", policyJsonArray);
		return policyJsonObj;
	}

	private static JSONObject writeState(StateVarTuple stateVarTuple) {
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

	private static JSONObject writeAction(IAction action) {
		JSONObject actionJsonObj = new JSONObject();
		actionJsonObj.put("type", action.getNamePrefix());
		JSONArray paramArray = new JSONArray();
		for (IStateVarValue paramValue : action.getParameters()) {
			if (paramValue instanceof IStateVarBoolean) {
				IStateVarBoolean paramBoolValue = (IStateVarBoolean) paramValue;
				paramArray.add(paramBoolValue.getValue());
			} else if (paramValue instanceof IStateVarInt) {
				IStateVarInt paramIntValue = (IStateVarInt) paramValue;
				paramArray.add(paramIntValue.getValue());
			} else if (paramValue instanceof IStateVarDouble) {
				IStateVarDouble paramDoubleValue = (IStateVarDouble) paramValue;
				paramArray.add(paramDoubleValue.getValue());
			} else {
				String paramValueStr = paramValue.toString();
				paramArray.add(paramValueStr);
			}
		}
		actionJsonObj.put("params", paramArray);
		return actionJsonObj;
	}
}
