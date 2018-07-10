package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVarDefinition;

public class MultivarClass implements Iterable<StateVarDefinition<IStateVarValue>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVarDefinition<? extends IStateVarValue>> mMultivarClass = new HashSet<>();

	public MultivarClass() {
		// mMultivarClass initially empty
	}

	public void add(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mMultivarClass.add(stateVarDef);
	}

	public boolean contains(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		return mMultivarClass.contains(stateVarDef);
	}

	public int size() {
		return mMultivarClass.size();
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return new Iterator<StateVarDefinition<IStateVarValue>>() {

			private Iterator<StateVarDefinition<? extends IStateVarValue>> iter = mMultivarClass.iterator();

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
		if (!(obj instanceof MultivarClass)) {
			return false;
		}
		MultivarClass multivarClass = (MultivarClass) obj;
		return multivarClass.mMultivarClass.equals(mMultivarClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMultivarClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
