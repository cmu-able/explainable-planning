package factors;

import java.util.HashMap;
import java.util.Map;

import exceptions.VarNameNotFoundException;

public class Context {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<String, StateVar<IStateVarValue>> mStateVars;

	public Context() {
		mStateVars = new HashMap<>();
	}

	public void addStateVar(StateVar<IStateVarValue> stateVar) {
		mStateVars.put(stateVar.getName(), stateVar);
	}

	public StateVar<IStateVarValue> getStateVar(String name) throws VarNameNotFoundException {
		if (!mStateVars.containsKey(name)) {
			throw new VarNameNotFoundException(name);
		}
		return mStateVars.get(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Context)) {
			return false;
		}
		Context context = (Context) obj;
		return context.mStateVars.equals(mStateVars);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVars.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
