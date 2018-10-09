package language.domain.metrics;

import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.IncompatibleActionException;
import language.exceptions.IncompatibleVarException;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarTuple;

/**
 * {@link Transition} represents a factored (s, a, s') transition. A transition has a parameterized type of
 * {@link ITransitionStructure}. That is, it has source and destination state variables and an action according to the
 * {@link ITransitionStructure}.
 * 
 * @author rsukkerd
 *
 */
public class Transition<E extends IAction, T extends ITransitionStructure<E>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private T mTransStructure;
	private E mAction;
	private StateVarTuple mSrcVarTuple = new StateVarTuple();
	private StateVarTuple mDestVarTuple = new StateVarTuple();

	public Transition(T transStructure, E action, StateVarTuple srcVars, StateVarTuple destVars)
			throws IncompatibleActionException, IncompatibleVarException {
		if (!sanityCheck(transStructure, action)) {
			throw new IncompatibleActionException(action);
		}
		mTransStructure = transStructure;
		mAction = action;
		for (StateVar<IStateVarValue> var : srcVars) {
			if (!sanityCheckSrc(transStructure, var)) {
				throw new IncompatibleVarException(var.getDefinition());
			}
			mSrcVarTuple.addStateVar(var);
		}
		for (StateVar<IStateVarValue> var : destVars) {
			if (!sanityCheckDest(transStructure, var)) {
				throw new IncompatibleVarException(var.getDefinition());
			}
			mDestVarTuple.addStateVar(var);
		}
	}

	private boolean sanityCheck(T domain, E action) {
		return domain.getActionDef().getActions().contains(action);
	}

	private boolean sanityCheckSrc(T domain, StateVar<? extends IStateVarValue> srcVar) {
		return domain.containsSrcStateVarDef(srcVar.getDefinition());
	}

	private boolean sanityCheckDest(T domain, StateVar<? extends IStateVarValue> destVar) {
		return domain.containsDestStateVarDef(destVar.getDefinition());
	}

	public E getAction() {
		return mAction;
	}

	public <S extends IStateVarValue> S getSrcStateVarValue(Class<S> valueType, StateVarDefinition<S> srcVarDef)
			throws VarNotFoundException {
		return mSrcVarTuple.getStateVarValue(valueType, srcVarDef);
	}

	public <S extends IStateVarValue> S getDestStateVarValue(Class<S> valueType, StateVarDefinition<S> destVarDef)
			throws VarNotFoundException {
		return mDestVarTuple.getStateVarValue(valueType, destVarDef);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Transition<?, ?>)) {
			return false;
		}
		Transition<?, ?> trans = (Transition<?, ?>) obj;
		return trans.mTransStructure.equals(mTransStructure) && trans.mAction.equals(mAction)
				&& trans.mSrcVarTuple.equals(mSrcVarTuple) && trans.mDestVarTuple.equals(mDestVarTuple);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTransStructure.hashCode();
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mSrcVarTuple.hashCode();
			result = 31 * result + mDestVarTuple.hashCode();
			hashCode = result;
		}
		return result;
	}
}
