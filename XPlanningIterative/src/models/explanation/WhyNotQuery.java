package models.explanation;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.mdp.StateVarTuple;

public class WhyNotQuery<E extends IAction, T extends ITransitionStructure<E>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarTuple mQueryState;
	private IAction mQueryAction;
	private IQFunction<E, T> mQueryQFunction;

	public WhyNotQuery(StateVarTuple queryState, IAction queryAction, IQFunction<E, T> queryQFunction) {
		mQueryState = queryState;
		mQueryAction = queryAction;
		mQueryQFunction = queryQFunction;
	}

	public StateVarTuple getQueryState() {
		return mQueryState;
	}

	public IAction getQueryAction() {
		return mQueryAction;
	}

	public IQFunction<E, T> getQueryQFunction() {
		return mQueryQFunction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof WhyNotQuery<?, ?>)) {
			return false;
		}
		WhyNotQuery<?, ?> query = (WhyNotQuery<?, ?>) obj;
		return query.mQueryState.equals(mQueryState) && query.mQueryAction.equals(mQueryAction)
				&& query.mQueryQFunction.equals(mQueryQFunction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQueryState.hashCode();
			result = 31 * result + mQueryAction.hashCode();
			result = 31 * result + mQueryQFunction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
