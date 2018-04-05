package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import exceptions.IncompatibleVarException;
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
	private EffectClass mEffectClass;

	public Effect(EffectClass effectClass) {
		mEffect = new HashSet<>();
		mEffectClass = effectClass;
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		StateVarDefinition<IStateVarValue> genericDef = (StateVarDefinition<IStateVarValue>) stateVar.getDefinition();
		StateVar<IStateVarValue> genericStateVar = new StateVar<>(genericDef, stateVar.getValue());
		if (!sanityCheck(genericStateVar)) {
			throw new IncompatibleVarException(genericDef);
		}
		mEffect.add(genericStateVar);
	}

	private boolean sanityCheck(StateVar<IStateVarValue> stateVar) {
		return mEffectClass.contains(stateVar.getDefinition());
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
