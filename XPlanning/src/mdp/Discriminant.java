package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVar;

public class Discriminant implements Iterable<StateVar<IStateVarValue>> {

	private Set<StateVar<IStateVarValue>> mDiscriminant;

	public Discriminant() {
		mDiscriminant = new HashSet<>();
	}

	public void add(StateVar<IStateVarValue> stateVar) {
		mDiscriminant.add(stateVar);
	}

	@Override
	public Iterator<StateVar<IStateVarValue>> iterator() {
		return mDiscriminant.iterator();
	}
}
