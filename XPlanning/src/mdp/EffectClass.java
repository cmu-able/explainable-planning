package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVarDefinition;

public class EffectClass implements Iterable<StateVarDefinition<IStateVarValue>> {

	private Set<StateVarDefinition<IStateVarValue>> mEffectClass;

	public EffectClass() {
		mEffectClass = new HashSet<>();
	}

	public void add(StateVarDefinition<IStateVarValue> stateVarDef) {
		mEffectClass.add(stateVarDef);
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return mEffectClass.iterator();
	}
}
