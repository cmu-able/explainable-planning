package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;

/**
 * {@link StatePredicate} is a predicate representing a set of states. It contains a set of state variables.
 * 
 * @author rsukkerd
 *
 */
public class StatePredicate implements IStatePredicate {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<StateVarDefinition<? extends IStateVarValue>, StateVar<? extends IStateVarValue>> mStateVarMap = new HashMap<>();

	public StatePredicate() {
		// mStateVarMap initially empty
	}

	public void addStateVar(StateVar<? extends IStateVarValue> stateVar) {
		mStateVarMap.put(stateVar.getDefinition(), stateVar);
	}

	@Override
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
		if (!(obj instanceof StatePredicate)) {
			return false;
		}
		StatePredicate state = (StatePredicate) obj;
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