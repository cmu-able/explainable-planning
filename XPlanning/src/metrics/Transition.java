package metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import exceptions.VarNotFoundException;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;

/**
 * {@link Transition} represents a factored (s, a, s') transition.
 * 
 * @author rsukkerd
 *
 */
public class Transition {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<StateVarDefinition<? extends IStateVarValue>, IStateVarValue> mSrcStateVars = new HashMap<>();
	private Map<StateVarDefinition<? extends IStateVarValue>, IStateVarValue> mDestStateVars = new HashMap<>();
	private IAction mAction;

	public Transition(IAction action) {
		mAction = action;
	}

	public Transition(IAction action, Set<StateVar<IStateVarValue>> srcVars, Set<StateVar<IStateVarValue>> destVars) {
		mAction = action;
		for (StateVar<IStateVarValue> var : srcVars) {
			mSrcStateVars.put(var.getDefinition(), var.getValue());
		}
		for (StateVar<IStateVarValue> var : destVars) {
			mDestStateVars.put(var.getDefinition(), var.getValue());
		}
	}

	public <E extends IStateVarValue> void addSrcStateVarValue(StateVarDefinition<E> stateVarDef, E value) {
		mSrcStateVars.put(stateVarDef, value);
	}

	public <E extends IStateVarValue> void addDestStateVarValue(StateVarDefinition<E> stateVarDef, E value) {
		mDestStateVars.put(stateVarDef, value);
	}

	public IStateVarValue getSrcStateVarValue(StateVarDefinition<? extends IStateVarValue> srcVarDef)
			throws VarNotFoundException {
		if (!mSrcStateVars.containsKey(srcVarDef)) {
			throw new VarNotFoundException(srcVarDef);
		}
		return mSrcStateVars.get(srcVarDef);
	}

	public IStateVarValue getDestStateVarValue(StateVarDefinition<? extends IStateVarValue> destVarDef)
			throws VarNotFoundException {
		if (!mDestStateVars.containsKey(destVarDef)) {
			throw new VarNotFoundException(destVarDef);
		}
		return mDestStateVars.get(destVarDef);
	}

	public IAction getAction() {
		return mAction;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Transition)) {
			return false;
		}
		Transition trans = (Transition) obj;
		return trans.mSrcStateVars.equals(mSrcStateVars) && trans.mDestStateVars.equals(mDestStateVars)
				&& trans.mAction.equals(mAction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSrcStateVars.hashCode();
			result = 31 * result + mDestStateVars.hashCode();
			result = 31 * result + mAction.hashCode();
			hashCode = result;
		}
		return result;
	}
}
