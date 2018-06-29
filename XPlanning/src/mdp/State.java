package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;

/**
 * {@link State} represents a structured state (i.e., a set of {@link StateVar}s).
 * 
 * @author rsukkerd
 *
 */
public class State implements IPredicate {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<StateVarDefinition<? extends IStateVarValue>, StateVar<? extends IStateVarValue>> mStateVarMap = new HashMap<>();

	public State() {
		// mStateVarMap initially empty
	}

	public void addStateVar(StateVar<? extends IStateVarValue> stateVar) {
		mStateVarMap.put(stateVar.getDefinition(), stateVar);
	}

	public boolean contains(StateVar<? extends IStateVarValue> stateVar) {
		return mStateVarMap.containsValue(stateVar);
	}

	public <E extends IStateVarValue> E getStateVarValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException {
		if (!mStateVarMap.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		return valueType.cast(mStateVarMap.get(stateVarDef).getValue());
	}

	@Override
	public Iterator<StateVar<IStateVarValue>> iterator() {
		return new Iterator<StateVar<IStateVarValue>>() {

			private Iterator<StateVar<? extends IStateVarValue>> iter = mStateVarMap.values().iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public StateVar<IStateVarValue> next() {
				return (StateVar<IStateVarValue>) iter.next();
			}
		};
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof State)) {
			return false;
		}
		State state = (State) obj;
		return state.mStateVarMap.equals(mStateVarMap);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarMap.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mStateVarMap.toString();
	}

}
