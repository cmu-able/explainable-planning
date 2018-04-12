package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.IAction;
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

	private EffectClass mEffectClass;
	private Map<StateVarDefinition<? extends IStateVarValue>, StateVar<? extends IStateVarValue>> mEffectVarMap;

	public Effect(EffectClass effectClass) {
		mEffectClass = effectClass;
		mEffectVarMap = new HashMap<>();
	}

	public Effect(IAction action, StateVarDefinition<? extends IStateVarValue>... stateVarDefs) {
		mEffectClass = new EffectClass(action);
		for (StateVarDefinition<? extends IStateVarValue> varDef : stateVarDefs) {
			mEffectClass.add(varDef);
		}
		mEffectVarMap = new HashMap<>();
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		if (!sanityCheck(stateVar)) {
			throw new IncompatibleVarException(stateVar.getDefinition());
		}
		mEffectVarMap.put(stateVar.getDefinition(), stateVar);
	}

	private boolean sanityCheck(StateVar<? extends IStateVarValue> stateVar) {
		return mEffectClass.contains(stateVar.getDefinition());
	}

	public <E extends IStateVarValue> E getEffectValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException {
		if (!mEffectVarMap.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		return valueType.cast(mEffectVarMap.get(stateVarDef).getValue());
	}

	public EffectClass getEffectClass() {
		return mEffectClass;
	}

	@Override
	public Iterator<StateVar<IStateVarValue>> iterator() {
		return new Iterator<StateVar<IStateVarValue>>() {

			private Iterator<StateVar<? extends IStateVarValue>> iter = mEffectVarMap.values().iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public StateVar<IStateVarValue> next() {
				return (StateVar<IStateVarValue>) iter.next();
			}
		};
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
		return effect.mEffectClass.equals(mEffectClass) && effect.mEffectVarMap.equals(mEffectVarMap);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEffectClass.hashCode();
			result = 31 * result + mEffectVarMap.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
