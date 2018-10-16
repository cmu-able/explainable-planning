package language.mdp;

import java.util.Iterator;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;

/**
 * {@link EffectClass} is a class of state variables that are dependently affected by a particular action. An action can
 * have different classes of effects that occur independently of each other.
 * 
 * @author rsukkerd
 *
 */
public class EffectClass implements IStateVarClass {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarClass mStateVarClass = new StateVarClass();

	public EffectClass() {
		// mEffectClass initially empty
	}

	public void add(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mStateVarClass.add(stateVarDef);
	}

	public void addAll(EffectClass effectClass) {
		mStateVarClass.addAll(effectClass.mStateVarClass);
	}

	public boolean contains(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		return mStateVarClass.contains(stateVarDef);
	}

	public boolean overlaps(EffectClass other) {
		return mStateVarClass.overlaps(other);
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return mStateVarClass.iterator();
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
		return effectClass.mStateVarClass.equals(mStateVarClass);
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
