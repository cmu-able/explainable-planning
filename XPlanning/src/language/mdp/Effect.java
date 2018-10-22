package language.mdp;

import java.util.Iterator;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.IncompatibleEffectClassException;
import language.exceptions.IncompatibleVarException;
import language.exceptions.VarNotFoundException;

/**
 * {@link Effect} is a set of state variables whose values changed from their previous values in the previous stage.
 * 
 * @author rsukkerd
 *
 */
public class Effect implements IStateVarTuple {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private EffectClass mEffectClass;
	private StateVarTuple mVarTuple = new StateVarTuple();

	public Effect(EffectClass effectClass) {
		mEffectClass = effectClass;
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		if (!sanityCheck(stateVar)) {
			throw new IncompatibleVarException(stateVar.getDefinition());
		}
		mVarTuple.addStateVar(stateVar);
	}

	public void addAll(Effect effect) throws IncompatibleEffectClassException {
		if (!sanityCheck(effect)) {
			throw new IncompatibleEffectClassException(effect.getEffectClass());
		}
		mVarTuple.addStateVarTuple(effect.mVarTuple);
	}

	private boolean sanityCheck(StateVar<? extends IStateVarValue> stateVar) {
		return mEffectClass.contains(stateVar.getDefinition());
	}

	private boolean sanityCheck(Effect effect) {
		for (StateVar<? extends IStateVarValue> stateVar : effect.mVarTuple) {
			if (!sanityCheck(stateVar)) {
				return false;
			}
		}
		return true;
	}

	public EffectClass getEffectClass() {
		return mEffectClass;
	}

	@Override
	public boolean isEmpty() {
		return mVarTuple.isEmpty();
	}

	@Override
	public boolean contains(IStateVarTuple stateVarTuple) {
		return mVarTuple.contains(stateVarTuple);
	}

	@Override
	public boolean contains(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		return mVarTuple.contains(stateVarDef);
	}

	@Override
	public <E extends IStateVarValue> E getStateVarValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException {
		return mVarTuple.getStateVarValue(valueType, stateVarDef);
	}

	@Override
	public Iterator<StateVar<IStateVarValue>> iterator() {
		return mVarTuple.iterator();
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
		return effect.mEffectClass.equals(mEffectClass) && effect.mVarTuple.equals(mVarTuple);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEffectClass.hashCode();
			result = 31 * result + mVarTuple.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
