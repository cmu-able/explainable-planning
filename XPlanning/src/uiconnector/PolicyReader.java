package uiconnector;

import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
		// FIXME
		String actionParam = PolicyJSONParserUtils.parseStringActionParameter(0, decisionJsonObj);
		String actionName = actionType + "(" + actionParam + ")";
		return mXMDP.getActionSpace().getAction(actionName);
	}
}
