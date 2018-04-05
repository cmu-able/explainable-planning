package mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;

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

	private Set<StateVar<IStateVarValue>> mEffect;
	private Map<StateVarDefinition<IStateVarValue>, IStateVarValue> mEffectValues;
	private EffectClass mEffectClass;

	public Effect(EffectClass effectClass) {
		mEffect = new HashSet<>();
		mEffectValues = new HashMap<>();
		mEffectClass = effectClass;
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		StateVarDefinition<IStateVarValue> genericVarDef = (StateVarDefinition<IStateVarValue>) stateVar
				.getDefinition();
		StateVar<IStateVarValue> genericVar = new StateVar<>(genericVarDef, stateVar.getValue());
		if (!sanityCheck(genericVar)) {
			throw new IncompatibleVarException(genericVarDef);
		}
		mEffect.add(genericVar);
		mEffectValues.put(genericVarDef, genericVar.getValue());
	}

	private boolean sanityCheck(StateVar<IStateVarValue> stateVar) {
		return mEffectClass.contains(stateVar.getDefinition());
	}

	public IStateVarValue getEffectValue(StateVarDefinition<? extends IStateVarValue> stateVarDef)
			throws VarNotFoundException {
		StateVarDefinition<IStateVarValue> genericVarDef = (StateVarDefinition<IStateVarValue>) stateVarDef;
		if (!mEffectValues.containsKey(genericVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		return mEffectValues.get(genericVarDef);
	}

	public EffectClass getEffectClass() {
		return mEffectClass;
	}

	@Override
	public Iterator<StateVar<IStateVarValue>> iterator() {
		return mEffect.iterator();
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
		return effect.mEffect.equals(mEffect) && effect.mEffectClass.equals(mEffectClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEffect.hashCode();
			result = 31 * result + mEffectClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
