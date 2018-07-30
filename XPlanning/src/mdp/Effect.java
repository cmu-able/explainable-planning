package mdp;

import java.util.Iterator;

import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import language.exceptions.IncompatibleVarException;
import language.exceptions.VarNotFoundException;

/**
 * {@link Effect} is a set of state variables whose values changed from their previous values in the previous stage.
 * 
 * @author rsukkerd
 *
 */
public class Effect implements Iterable<StateVar<IStateVarValue>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private EffectClass mEffectClass;
	private StateVarTuple mState = new StateVarTuple();

	public Effect(EffectClass effectClass) {
		mEffectClass = effectClass;
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		if (!sanityCheck(stateVar)) {
			throw new IncompatibleVarException(stateVar.getDefinition());
		}
		mState.addStateVar(stateVar);
	}

	private boolean sanityCheck(StateVar<? extends IStateVarValue> stateVar) {
		return mEffectClass.contains(stateVar.getDefinition());
	}

	public void addAll(Effect effect) throws IncompatibleVarException {
		for (StateVar<IStateVarValue> stateVar : effect) {
			add(stateVar);
		}
	}

	public <E extends IStateVarValue> E getEffectValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException {
		return mState.getStateVarValue(valueType, stateVarDef);
	}

	public EffectClass getEffectClass() {
		return mEffectClass;
	}

	@Override
	public Iterator<StateVar<IStateVarValue>> iterator() {
		return mState.iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Effect)) {
			return false;
		}
		Effect effect = (Effect) obj;
		return effect.mEffectClass.equals(mEffectClass) && effect.mState.equals(mState);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEffectClass.hashCode();
			result = 31 * result + mState.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
