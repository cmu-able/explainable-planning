package language.mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;

/**
 * {@link StateSpace} represents a state space (i.e., a set of {@link StateVarDefinition}s) of an MDP.
 * 
 * @author rsukkerd
 *
 */
public class StateSpace implements Iterable<StateVarDefinition<IStateVarValue>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVarDefinition<? extends IStateVarValue>> mStateVarDefs = new HashSet<>();

	// For fast look-up
	private Map<String, StateVarDefinition<? extends IStateVarValue>> mStateVarDefsLookup = new HashMap<>();

	public StateSpace() {
		// mStateVarDefs initially empty
	}

	public void addStateVarDefinition(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mStateVarDefs.add(stateVarDef);
		mStateVarDefsLookup.put(stateVarDef.getName(), stateVarDef);
	}

	public void addStateVarDefinitions(Iterable<StateVarDefinition<IStateVarValue>> stateVarDefs) {
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateVarDefs) {
			addStateVarDefinition(stateVarDef);
		}
	}

	public <E extends IStateVarValue> StateVarDefinition<E> getStateVarDefinition(String stateVarName) {
		return (StateVarDefinition<E>) mStateVarDefsLookup.get(stateVarName);
	}

	/**
	 * 
	 * @param stateSpace
	 * @return The difference of this state space from the given state space
	 */
	public StateSpace getDifference(StateSpace stateSpace) {
		StateSpace diff = new StateSpace();
		for (StateVarDefinition<? extends IStateVarValue> stateVarDef : mStateVarDefs) {
			if (!stateSpace.mStateVarDefs.contains(stateVarDef)) {
				diff.addStateVarDefinition(stateVarDef);
			}
		}
		return diff;
	}

	public boolean isEmpty() {
		return mStateVarDefs.isEmpty();
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return new Iterator<StateVarDefinition<IStateVarValue>>() {

			private Iterator<StateVarDefinition<? extends IStateVarValue>> iter = mStateVarDefs.iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public StateVarDefinition<IStateVarValue> next() {
				return (StateVarDefinition<IStateVarValue>) iter.next();
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
		if (!(obj instanceof StateSpace)) {
			return false;
		}
		StateSpace stateSpace = (StateSpace) obj;
		return stateSpace.mStateVarDefs.equals(mStateVarDefs);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarDefs.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
