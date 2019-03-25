package uiconnector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
		// Convert action parameters from type IStateVarValue to String for JSON output
		List<String> actionParamStrArray = new ArrayList<>();
		for (IStateVarValue paramValue : action.getParameters()) {
			String paramValueStr = paramValue.toString();
			actionParamStrArray.add(paramValueStr);
		}

		JSONObject actionJsonObj = new JSONObject();
		actionJsonObj.put("type", action.getNamePrefix());
		JSONArray paramArray = new JSONArray();
		paramArray.addAll(actionParamStrArray);
		actionJsonObj.put("params", paramArray);
		return actionJsonObj;
	}
}
