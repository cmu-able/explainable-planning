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

	private Set<StateVarDefinition<IStateVarValue>> mStateVarDefs;

	public StateSpace() {
		mStateVarDefs = new HashSet<>();
	}

	public void addStateVarDefinition(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		StateVarDefinition<IStateVarValue> genericVarDef = new StateVarDefinition<>(stateVarDef.getName(),
				stateVarDef.getPossibleValues());
		mStateVarDefs.add(genericVarDef);
	}

	public void addStateVarDefinitions(Set<StateVarDefinition<IStateVarValue>> stateVarDefs) {
		mStateVarDefs.addAll(stateVarDefs);
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return mStateVarDefs.iterator();
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