package uiconnector;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import language.domain.models.IAction;
import language.domain.models.IStateVarBoolean;
import language.domain.models.IStateVarDouble;
import language.domain.models.IStateVarInt;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.policy.Policy;

public class PolicyReader {

	private XMDP mXMDP;

	public PolicyReader(XMDP xmdp) {
		mXMDP = xmdp;
	}

	public Policy readPolicy(File policyJsonFile) throws IOException, ParseException {
		FileReader reader = new FileReader(policyJsonFile);
		JSONParser jsonParser = new JSONParser();
		JSONObject policyJsonObj = (JSONObject) jsonParser.parse(reader);
		return readPolicy(policyJsonObj);
	}

	public Policy readPolicy(JSONObject policyJsonObj) {
		Policy policy = new Policy();
		JSONArray policyJsonArray = (JSONArray) policyJsonObj.get("policy");
		for (Object obj : policyJsonArray) {
			JSONObject decisionJsonObj = (JSONObject) obj;

			StateVarTuple state = readState(decisionJsonObj);
			IAction action = readAction(decisionJsonObj);
			policy.put(state, action);
		}
		return policy;
	}

	private StateVarTuple readState(JSONObject decisionJsonObj) {
		StateVarTuple state = new StateVarTuple();
		for (StateVarDefinition<IStateVarValue> varDef : mXMDP.getStateSpace()) {
			String varName = varDef.getName();

			Iterator<IStateVarValue> iter = varDef.getPossibleValues().iterator();
			boolean addedVar = false;

			while (!addedVar && iter.hasNext()) {
				IStateVarValue value = iter.next();

				if (value instanceof IStateVarBoolean) {
					IStateVarBoolean boolValue = (IStateVarBoolean) value;
					boolean bv = PolicyJSONParserUtils.parseBooleanVar(varName, decisionJsonObj);

					addedVar = (addStateVar(state, varDef, boolValue, Boolean.valueOf(boolValue.getValue()),
							Boolean.valueOf(bv)));
				} else if (value instanceof IStateVarInt) {
					IStateVarInt intValue = (IStateVarInt) value;
					int iv = PolicyJSONParserUtils.parseIntVar(varName, decisionJsonObj);

					addedVar = addStateVar(state, varDef, intValue, Integer.valueOf(intValue.getValue()),
							Integer.valueOf(iv));
				} else if (value instanceof IStateVarDouble) {
					IStateVarDouble doubleValue = (IStateVarDouble) value;
					double dv = PolicyJSONParserUtils.parseDoubleVar(varName, decisionJsonObj);

					addedVar = addStateVar(state, varDef, doubleValue, Double.valueOf(doubleValue.getValue()),
							Double.valueOf(dv));
				} else {
					String sv = PolicyJSONParserUtils.parseStringVar(varName, decisionJsonObj);

					addedVar = addStateVar(state, varDef, value, value.toString(), sv);
				}
			}
		}
		return state;
	}

	private boolean addStateVar(StateVarTuple state, StateVarDefinition<IStateVarValue> varDef, IStateVarValue value,
			Object objA, Object objB) {
		if (objA.equals(objB)) {
			StateVar<IStateVarValue> var = varDef.getStateVar(value);
			state.addStateVar(var);
		}
		return objA.equals(objB);
	}

	private IAction readAction(JSONObject decisionJsonObj) {
		String actionType = PolicyJSONParserUtils.parseActionType(decisionJsonObj);
		int numActionParams = PolicyJSONParserUtils.getNumActionParameters(decisionJsonObj);
		String[] actionParams = new String[numActionParams];
		for (int i = 0; i < numActionParams; i++) {
			Class<?> actionParamType = PolicyJSONParserUtils.getActionParameterType(i, decisionJsonObj);
			String actionParam;
			if (actionParamType.equals(Boolean.class)) {
				boolean boolParam = PolicyJSONParserUtils.parseBooleanActionParameter(i, decisionJsonObj);
				actionParam = String.valueOf(boolParam);
			} else if (actionParamType.equals(Long.class)) {
				// JSONSimple uses "long" type for whole numbers
				int intParam = PolicyJSONParserUtils.parseIntActionParameter(i, decisionJsonObj);
				actionParam = String.valueOf(intParam);
			} else if (actionParamType.equals(Double.class)) {
				double doubleParam = PolicyJSONParserUtils.parseDoubleActionParameter(i, decisionJsonObj);
				actionParam = String.valueOf(doubleParam);
			} else {
				actionParam = PolicyJSONParserUtils.parseStringActionParameter(i, decisionJsonObj);
			}

			actionParams[i] = actionParam;
		}

		String actionName = actionType + "(" + String.join(",", actionParams) + ")";
		return mXMDP.getActionSpace().getAction(actionName);
	}
}
