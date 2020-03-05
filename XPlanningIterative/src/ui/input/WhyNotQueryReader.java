package ui.input;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.mdp.StateVarTuple;
import models.explanation.WhyNotQuery;

public class WhyNotQueryReader {

	public <E extends IAction, T extends ITransitionStructure<E>> WhyNotQuery<E, T> readWhyNotQuery(
			String queryStateStr, String queryActionStr, String queryQFunctionStr) {
		return null;
	}

	private StateVarTuple readQueryState(String queryStateStr) {
		return null;
	}

	private IAction readQueryAction(String queryActionStr) {
		return null;
	}

	private <E extends IAction, T extends ITransitionStructure<E>> IQFunction<E, T> readQueryQFunction(
			String queryQFunctionStr) {
		return null;
	}
}
