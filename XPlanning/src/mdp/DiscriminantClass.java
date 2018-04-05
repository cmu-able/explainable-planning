package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVarDefinition;

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

	private Set<StateVarDefinition<IStateVarValue>> mDiscriminantClass;

	public DiscriminantClass() {
		mDiscriminantClass = new HashSet<>();
	}

	public void add(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		Set<IStateVarValue> genericValues = new HashSet<>(stateVarDef.getPossibleValues());
		StateVarDefinition<IStateVarValue> genericVarDef = new StateVarDefinition<>(stateVarDef.getName(),
				genericValues);
		mDiscriminantClass.add(genericVarDef);
	}

	public boolean contains(StateVarDefinition<IStateVarValue> stateVarDef) {
		return mDiscriminantClass.contains(stateVarDef);
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return mDiscriminantClass.iterator();
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
