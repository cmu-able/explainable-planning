package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;

/**
 * {@link Discriminant} determines what effect an action will have. Each action has a finite set of mutually exclusive
 * and exhaustive discriminants (propositions). Each discriminant is associated with a {@link ProbabilisticEffect}.
 * 
 * An iterator of a discriminant is over a minimal collection of state variables whose values satisfy the proposition of
 * the discriminant.
 * 
 * @author rsukkerd
 *
 */
public class Discriminant implements Iterable<StateVar<IStateVarValue>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private DiscriminantClass mDiscrClass;
	private Map<StateVarDefinition<? extends IStateVarValue>, StateVar<? extends IStateVarValue>> mDiscrVarMap;

	public Discriminant(DiscriminantClass discriminantClass) {
		mDiscrClass = discriminantClass;
		mDiscrVarMap = new HashMap<>();
	}

	public Discriminant(StateVarDefinition<? extends IStateVarValue>... stateVarDefs) {
		mDiscrClass = new DiscriminantClass();
		for (StateVarDefinition<? extends IStateVarValue> varDef : stateVarDefs) {
			mDiscrClass.add(varDef);
		}
		mDiscrVarMap = new HashMap<>();
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		if (!sanityCheck(stateVar)) {
			throw new IncompatibleVarException(stateVar.getDefinition());
		}
		mDiscrVarMap.put(stateVar.getDefinition(), stateVar);
	}

	private boolean sanityCheck(StateVar<? extends IStateVarValue> stateVar) {
		return mDiscrClass.contains(stateVar.getDefinition());
	}

	public <E extends IStateVarValue> E getDiscriminantValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException {
		if (!mDiscrVarMap.containsKey(stateVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		return valueType.cast(mDiscrVarMap.get(stateVarDef).getValue());
	}

	public DiscriminantClass getDiscriminantClass() {
		return mDiscrClass;
	}

	@Override
	public Iterator<StateVar<IStateVarValue>> iterator() {
		return new Iterator<StateVar<IStateVarValue>>() {

			private Iterator<StateVar<? extends IStateVarValue>> iter = mDiscrVarMap.values().iterator();

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
		if (!(obj instanceof Discriminant)) {
			return false;
		}
		Discriminant discr = (Discriminant) obj;
		return discr.mDiscrClass.equals(mDiscrClass) && discr.mDiscrVarMap.equals(mDiscrVarMap);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDiscrClass.hashCode();
			result = 31 * result + mDiscrVarMap.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
