package language.mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;

/**
 * {@link DiscriminantClass} is a set of {@link StateVarDefinition} that defines a class of {@link Discriminant}s.
 * 
 * @author rsukkerd
 *
 */
public class DiscriminantClass implements Iterable<StateVarDefinition<IStateVarValue>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVarDefinition<? extends IStateVarValue>> mDiscriminantClass = new HashSet<>();

	public DiscriminantClass() {
		// mDiscriminantClass initially empty
	}

	public void add(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mDiscriminantClass.add(stateVarDef);
	}

	public void addAll(DiscriminantClass discriminantClass) {
		mDiscriminantClass.addAll(discriminantClass.mDiscriminantClass);
	}

	public boolean contains(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		return mDiscriminantClass.contains(stateVarDef);
	}

	public boolean isEmpty() {
		return mDiscriminantClass.isEmpty();
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return new Iterator<StateVarDefinition<IStateVarValue>>() {

			private Iterator<StateVarDefinition<? extends IStateVarValue>> iter = mDiscriminantClass.iterator();

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
		if (!(obj instanceof DiscriminantClass)) {
			return false;
		}
		DiscriminantClass discrClass = (DiscriminantClass) obj;
		return discrClass.mDiscriminantClass.equals(mDiscriminantClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDiscriminantClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
