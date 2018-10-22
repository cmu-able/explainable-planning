package language.domain.metrics;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.mdp.StateVarClass;

/**
 * {@link TransitionStructure} represents the structure of a transition. It contains a set of variable definitions in
 * the source state, a set in the destination state, and an action definition. It can be used to represent the domain of
 * a {@link IQFunction} -- among others. This is to facilitate PRISM translator in generating a reward structure for the
 * corresponding QA function.
 * 
 * Assumption: A source state variable class is either a class of independent variables, or a class of all-dependent
 * variables, w.r.t. action precondition. Such all-dependent variables can be a proper subset of the variables in a
 * multivariate predicate in the action precondition.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class TransitionStructure<E extends IAction> implements ITransitionStructure<E> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarClass mSrcVarClass = new StateVarClass();
	private StateVarClass mDestVarClass = new StateVarClass();
	private ActionDefinition<E> mActionDef;

	public void addSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mSrcVarClass.add(stateVarDef);
	}

	public void addDestStateVarDef(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mDestVarClass.add(stateVarDef);
	}

	public void setActionDef(ActionDefinition<E> actionDef) {
		mActionDef = actionDef;
	}

	@Override
	public StateVarClass getSrcStateVarClass() {
		return mSrcVarClass;
	}

	@Override
	public StateVarClass getDestStateVarClass() {
		return mDestVarClass;
	}

	@Override
	public ActionDefinition<E> getActionDef() {
		return mActionDef;
	}

	@Override
	public boolean containsSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> srcVarDef) {
		return mSrcVarClass.contains(srcVarDef);
	}

	@Override
	public boolean containsDestStateVarDef(StateVarDefinition<? extends IStateVarValue> destVarDef) {
		return mDestVarClass.contains(destVarDef);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TransitionStructure<?>)) {
			return false;
		}
		TransitionStructure<?> domain = (TransitionStructure<?>) obj;
		return domain.mSrcVarClass.equals(mSrcVarClass) && domain.mDestVarClass.equals(mDestVarClass)
				&& domain.mActionDef.equals(mActionDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSrcVarClass.hashCode();
			result = 31 * result + mDestVarClass.hashCode();
			result = 31 * result + mActionDef.hashCode();
			hashCode = result;
		}
		return result;
	}

}
