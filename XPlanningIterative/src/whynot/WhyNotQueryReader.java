package whynot;

import java.util.ArrayList;
import java.util.List;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import models.explanation.WhyNotQuery;

public class WhyNotQueryReader {

	private XMDP mXMDP;

	public WhyNotQueryReader(XMDP xmdp) {
		mXMDP = xmdp;
	}

	/**
	 * 
	 * @param whyNotQueryStr
	 *            : Format: query state;query action;query QA
	 * @return Why-not query
	 */
	public <E extends IAction, T extends ITransitionStructure<E>> WhyNotQuery<E, T> readWhyNotQuery(
			String whyNotQueryStr) {
		String[] queryStrs = whyNotQueryStr.split(";");
		return readWhyNotQuery(queryStrs[0], queryStrs[1], queryStrs[2]);
	}

	public <E extends IAction, T extends ITransitionStructure<E>> WhyNotQuery<E, T> readWhyNotQuery(
			String queryStateStr, String queryActionStr, String queryQFunctionStr) {
		StateVarTuple queryState = readQueryState(queryStateStr);
		List<IAction> queryActions = readQueryActions(queryActionStr);
		IQFunction<E, T> queryQFunction = readQueryQFunction(queryQFunctionStr);
		return new WhyNotQuery<>(queryState, queryActions, queryQFunction);
	}

	/**
	 * 
	 * @param queryStateStr
	 *            : Format: varName1=value1,varName2=value2,...,varNameN=valueN
	 * @return Query state
	 */
	private StateVarTuple readQueryState(String queryStateStr) {
		StateVarTuple queryState = new StateVarTuple();

		String[] stateVars = queryStateStr.split(",");
		for (String stateVarStr : stateVars) {
			String[] pair = stateVarStr.split("=");
			String stateVarName = pair[0];
			String valueStr = pair[1];

			StateVarDefinition<IStateVarValue> stateVarDef = mXMDP.getStateSpace().getStateVarDefinition(stateVarName);
			IStateVarValue value = null;
			for (IStateVarValue possibleValue : stateVarDef.getPossibleValues()) {
				String possibleValueStr = possibleValue.toString();
				if (possibleValueStr.equals(valueStr)) {
					value = possibleValue;
					break;
				}
			}

			StateVar<IStateVarValue> stateVar = stateVarDef.getStateVar(value);
			queryState.addStateVar(stateVar);
		}

		return queryState;
	}

	private List<IAction> readQueryActions(String queryActionStr) {
		List<IAction> queryActions = new ArrayList<>();

		String[] actions = queryActionStr.split(",");
		for (String actionStr : actions) {
			IAction queryAction = mXMDP.getActionSpace().getAction(actionStr);
			queryActions.add(queryAction);
		}

		return queryActions;
	}

	private <E extends IAction, T extends ITransitionStructure<E>> IQFunction<E, T> readQueryQFunction(
			String queryQFunctionStr) {
		return mXMDP.getQSpace().getQFunction(IQFunction.class, queryQFunctionStr);
	}
}
