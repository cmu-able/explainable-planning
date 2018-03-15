package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.IAction;
import factors.IStateVarValue;
import factors.StateVarDefinition;

public class EffectClass implements Iterable<StateVarDefinition<IStateVarValue>> {

	private Set<StateVarDefinition<IStateVarValue>> mEffectClass;
	private IAction mAction;

	public EffectClass(IAction action) {
		mEffectClass = new HashSet<>();
		mAction = action;
	}

	public void add(StateVarDefinition<IStateVarValue> stateVarDef) {
		mEffectClass.add(stateVarDef);
	}

	public Set<StateVarDefinition<IStateVarValue>> getAffectedVarDefs() {
		return mEffectClass;
	}

	public IAction getAction() {
		return mAction;
	}

	public boolean overlaps(EffectClass other) {
		for (StateVarDefinition<IStateVarValue> varDef : other) {
			if (mEffectClass.contains(varDef)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return mEffectClass.iterator();
	}
}
