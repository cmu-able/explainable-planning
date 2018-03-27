package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.IAction;
import factors.IStateVarValue;
import factors.StateVarDefinition;

public class DiscriminantClass implements Iterable<StateVarDefinition<IStateVarValue>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVarDefinition<IStateVarValue>> mDiscriminantClass;
	private IAction mAction;

	public DiscriminantClass(IAction action) {
		mDiscriminantClass = new HashSet<>();
		mAction = action;
	}

	public void add(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		Set<IStateVarValue> genericValues = new HashSet<>(stateVarDef.getPossibleValues());
		StateVarDefinition<IStateVarValue> genericVarDef = new StateVarDefinition<>(stateVarDef.getName(),
				genericValues);
		mDiscriminantClass.add(genericVarDef);
	}

	public IAction getAction() {
		return mAction;
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
		return discrClass.mDiscriminantClass.equals(mDiscriminantClass) && discrClass.mAction.equals(mAction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDiscriminantClass.hashCode();
			result = 31 * result + mAction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
