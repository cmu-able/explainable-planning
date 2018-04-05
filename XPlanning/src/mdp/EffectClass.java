package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import factors.IAction;
import factors.IStateVarValue;
import factors.StateVarDefinition;

/**
 * {@link EffectClass} is a class of state variables that are dependently affected by a particular action. An action can
 * have different classes of effects that occur independently of each other.
 * 
 * @author rsukkerd
 *
 */
public class EffectClass implements Iterable<StateVarDefinition<IStateVarValue>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Set<StateVarDefinition<IStateVarValue>> mEffectClass;
	private IAction mAction;

	public EffectClass(IAction action) {
		mEffectClass = new HashSet<>();
		mAction = action;
	}

	public void add(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		Set<IStateVarValue> genericValues = new HashSet<>(stateVarDef.getPossibleValues());
		StateVarDefinition<IStateVarValue> genericVarDef = new StateVarDefinition<>(stateVarDef.getName(),
				genericValues);
		mEffectClass.add(genericVarDef);
	}

	public Set<StateVarDefinition<IStateVarValue>> getAllVarDefs() {
		return mEffectClass;
	}

	public IAction getAction() {
		return mAction;
	}

	public boolean contains(StateVarDefinition<IStateVarValue> stateVarDef) {
		return mEffectClass.contains(stateVarDef);
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof EffectClass)) {
			return false;
		}
		EffectClass effectClass = (EffectClass) obj;
		return effectClass.mEffectClass.equals(mEffectClass) && effectClass.mAction.equals(mAction);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEffectClass.hashCode();
			result = 31 * result + mAction.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
