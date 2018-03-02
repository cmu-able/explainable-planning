package metrics;

import java.util.HashMap;
import java.util.Map;

import exceptions.VarNameNotFoundException;
import factors.IAction;
import factors.IStateVarValue;

/**
 * {@link Transition} represents a (s, a, s') transition.
 * 
 * @author rsukkerd
 *
 */
public class Transition {

	private Map<String, IStateVarValue> mSrcStateVars = new HashMap<>();
	private Map<String, IStateVarValue> mDestStateVars = new HashMap<>();
	private IAction mAction;
	
	public Transition(Map<String, IStateVarValue> srcStateVars, IAction action, Map<String, IStateVarValue> destStateVars) {
		mSrcStateVars = srcStateVars;
		mDestStateVars = destStateVars;
		mAction = action;
	}
	
	public IStateVarValue getSrcStateVar(String srcVarName) throws VarNameNotFoundException {
		if (!mSrcStateVars.containsKey(srcVarName)) {
			throw new VarNameNotFoundException(srcVarName);
		}
		return mSrcStateVars.get(srcVarName);
	}
	
	public IStateVarValue getDestStateVar(String destVarName) throws VarNameNotFoundException {
		if (!mDestStateVars.containsKey(destVarName)) {
			throw new VarNameNotFoundException(destVarName);
		}
		return mDestStateVars.get(destVarName);
	}
	
	public IAction getAction() {
		return mAction;
	}
}
