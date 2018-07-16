package metrics;

import java.util.Set;

import exceptions.IncompatibleActionException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.StateVarTuple;

/**
 * {@link Transition} represents a factored (s, a, s') transition.
 * 
 * @author rsukkerd
 *
 */
public class Transition<E extends IAction, T extends IQFunctionDomain<E>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private T mDomain;
	private E mAction;
	private StateVarTuple mSrcVarTuple = new StateVarTuple();
	private StateVarTuple mDestVarTuple = new StateVarTuple();

	public Transition(T domain, E action, Set<StateVar<? extends IStateVarValue>> srcVars,
			Set<StateVar<? extends IStateVarValue>> destVars)
			throws IncompatibleActionException, IncompatibleVarException {
		if (!sanityCheck(domain, action)) {
			throw new IncompatibleActionException(action);
		}
		mDomain = domain;
		mAction = action;
		for (StateVar<? extends IStateVarValue> var : srcVars) {
			if (!sanityCheckSrc(domain, var)) {
				throw new IncompatibleVarException(var.getDefinition());
			}
			mSrcVarTuple.addStateVar(var);
		}
		for (StateVar<? extends IStateVarValue> var : destVars) {
			if (!sanityCheckDest(domain, var)) {
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

	public T getQFunctionDomain() {
		return mDomain;
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
		return trans.mDomain.equals(mDomain) && trans.mAction.equals(mAction) && trans.mSrcVarTuple.equals(mSrcVarTuple)
				&& trans.mDestVarTuple.equals(mDestVarTuple);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mSrcVarTuple.hashCode();
			result = 31 * result + mDestVarTuple.hashCode();
			hashCode = result;
		}
		return result;
	}
}
