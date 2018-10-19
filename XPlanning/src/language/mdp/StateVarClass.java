package language.mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;

/**
 * {@link StateVarClass} is a class of state variables that have some dependency relationship among each other.
 * 
 * @author rsukkerd
 *
 */
public class StateVarClass implements IStateVarClass {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVarDefinition<? extends IStateVarValue>> mStateVarClass = new HashSet<>();

	public StateVarClass() {
		// mStateVarClass initially empty
	}

	public void add(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mStateVarClass.add(stateVarDef);
	}

	public void addAll(StateVarClass stateVarClass) {
		mStateVarClass.addAll(stateVarClass.mStateVarClass);
	}

	public boolean contains(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		return mStateVarClass.contains(stateVarDef);
	}

	public boolean containsAll(StateVarClass stateVarClass) {
		return mStateVarClass.containsAll(stateVarClass.mStateVarClass);
	}

	public boolean isEmpty() {
		return mStateVarClass.isEmpty();
	}

	public boolean overlaps(IStateVarClass other) {
		for (StateVarDefinition<? extends IStateVarValue> varDef : other) {
			if (mStateVarClass.contains(varDef)) {
				return true;
			}
		}
		return false;
	}

	public int size() {
		return mStateVarClass.size();
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return new Iterator<StateVarDefinition<IStateVarValue>>() {

			private Iterator<StateVarDefinition<? extends IStateVarValue>> iter = mStateVarClass.iterator();

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
		if (!(obj instanceof StateVarClass)) {
			return false;
		}
		StateVarClass stateVarClass = (StateVarClass) obj;
		return stateVarClass.mStateVarClass.equals(mStateVarClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
