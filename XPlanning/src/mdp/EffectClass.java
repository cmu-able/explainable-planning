package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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

	private Set<StateVarDefinition<? extends IStateVarValue>> mEffectClass = new HashSet<>();

	public EffectClass() {
		// mEffectClass initially empty
	}

	public void add(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mEffectClass.add(stateVarDef);
	}

	public void addAll(EffectClass effectClass) {
		for (StateVarDefinition<IStateVarValue> stateVarDef : effectClass) {
			mEffectClass.add(stateVarDef);
		}
	}

	public boolean contains(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		return mEffectClass.contains(stateVarDef);
	}

	public boolean overlaps(EffectClass other) {
		for (StateVarDefinition<? extends IStateVarValue> varDef : other.mEffectClass) {
			if (mEffectClass.contains(varDef)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return new Iterator<StateVarDefinition<IStateVarValue>>() {

			private Iterator<StateVarDefinition<? extends IStateVarValue>> iter = mEffectClass.iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public StateVarDefinition<IStateVarValue> next() {
				return (StateVarDefinition<IStateVarValue>) iter.next();
			}
		};
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
		return effectClass.mEffectClass.equals(mEffectClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEffectClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
