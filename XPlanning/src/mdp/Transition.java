package mdp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import exceptions.VarNameNotFoundException;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;

/**
 * {@link Transition} represents a (s, a, s') transition.
 * 
 * @author rsukkerd
 *
 */
public class Transition {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<String, StateVar<IStateVarValue>> mSrcStateVars = new HashMap<>();
	private Map<String, StateVar<IStateVarValue>> mDestStateVars = new HashMap<>();
	private IAction mAction;

	public Transition(Set<StateVar<IStateVarValue>> srcStateVars, IAction action,
			Set<StateVar<IStateVarValue>> destStateVars) {
		for (StateVar<IStateVarValue> var : srcStateVars) {
			mSrcStateVars.put(var.getName(), var);
		}
		for (StateVar<IStateVarValue> var : destStateVars) {
			mDestStateVars.put(var.getName(), var);
		}
		mAction = action;
	}

	public StateVar<IStateVarValue> getSrcStateVar(String srcVarName) throws VarNameNotFoundException {
		if (!mSrcStateVars.containsKey(srcVarName)) {
			throw new VarNameNotFoundException(srcVarName);
		}
		return mSrcStateVars.get(srcVarName);
	}

	public StateVar<IStateVarValue> getDestStateVar(String destVarName) throws VarNameNotFoundException {
		if (!mDestStateVars.containsKey(destVarName)) {
			throw new VarNameNotFoundException(destVarName);
		}
		return mDestStateVars.get(destVarName);
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
