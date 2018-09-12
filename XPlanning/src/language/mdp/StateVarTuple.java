package language.mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import language.exceptions.VarNotFoundException;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;
import language.qfactors.StateVarDefinition;

/**
 * {@link StateVarTuple} is is a tuple (v1,...,vk) of state variables. It defines at most 1 allowable value for each
 * state variable. Undefined state variables can have any value.
 * 
 * @author rsukkerd
 *
 */
public class StateVarTuple implements IStateVarTuple {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<StateVarDefinition<? extends IStateVarValue>, StateVar<? extends IStateVarValue>> mStateVarMap = new HashMap<>();

	public StateVarTuple() {
		// mStateVarMap and mMultivarClass initially empty
	}

	public void addStateVar(StateVar<? extends IStateVarValue> stateVar) {
		mStateVarMap.put(stateVar.getDefinition(), stateVar);
	}

	public void addStateVarTuple(StateVarTuple stateVarTuple) {
		mStateVarMap.putAll(stateVarTuple.mStateVarMap);
	}

	@Override
	public boolean contains(IStateVarTuple stateVarTuple) {
		for (StateVar<IStateVarValue> stateVar : stateVarTuple) {
			if (!mStateVarMap.containsValue(stateVar)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean contains(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		return mStateVarMap.containsKey(stateVarDef);
	}

	@Override
	public <E extends IStateVarValue> E getStateVarValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException {
		if (!contains(stateVarDef)) {
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

			@Override
			public void remove() {
				iter.remove();
			}
		};
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof StateVarTuple)) {
			return false;
		}
		StateVarTuple state = (StateVarTuple) obj;
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
