package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVarDefinition;

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

	private Set<StateVarDefinition<? extends IStateVarValue>> mStateVarDefs;

	public StateSpace() {
		mStateVarDefs = new HashSet<>();
	}

	public void addStateVarDefinition(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mStateVarDefs.add(stateVarDef);
	}

	public void addStateVarDefinitions(Iterable<StateVarDefinition<IStateVarValue>> stateVarDefs) {
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateVarDefs) {
			mStateVarDefs.add(stateVarDef);
		}
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
