package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;

/**
 * {@link State} represents a structured state (i.e., a set of {@link StateVar}s).
 * 
 * @author rsukkerd
 *
 */
public class State implements Iterable<StateVar<IStateVarValue>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<StateVarDefinition<? extends IStateVarValue>, StateVar<? extends IStateVarValue>> mStateVarMap;

	public State() {
		mStateVarMap = new HashMap<>();
	}

	public void addStateVar(StateVar<? extends IStateVarValue> stateVar) {
		mStateVarMap.put(stateVar.getDefinition(), stateVar);
	}

	public <E extends IStateVarValue> StateVar<E> getStateVar(StateVarDefinition<E> stateVarDef) {
		StateVar<E> stateVar = (StateVar<E>) mStateVarMap.get(stateVarDef);
		return stateVar;
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
}
