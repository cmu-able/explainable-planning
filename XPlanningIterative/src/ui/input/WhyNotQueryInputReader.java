package ui.input;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.mdp.StateVarTuple;
import models.explanation.WhyNotQuery;

public class WhyNotQueryInputReader {

	public <E extends IAction, T extends ITransitionStructure<E>> WhyNotQuery<E, T> readWhyNotQuery(
			String whyNotQueryStr) {
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
