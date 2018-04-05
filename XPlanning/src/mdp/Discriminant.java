package mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.IAction;
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

	private Set<StateVar<IStateVarValue>> mDiscriminant;
	private Map<StateVarDefinition<IStateVarValue>, IStateVarValue> mDiscriminantValues;
	private DiscriminantClass mDiscriminantClass;

	public Discriminant(DiscriminantClass discriminantClass) {
		mDiscriminant = new HashSet<>();
		mDiscriminantValues = new HashMap<>();
		mDiscriminantClass = discriminantClass;
	}

	public Discriminant(IAction action, StateVarDefinition<? extends IStateVarValue>... stateVarDefs) {
		mDiscriminant = new HashSet<>();
		mDiscriminantValues = new HashMap<>();
		mDiscriminantClass = new DiscriminantClass(action);
		for (StateVarDefinition<? extends IStateVarValue> varDef : stateVarDefs) {
			mDiscriminantClass.add(varDef);
		}
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		StateVarDefinition<IStateVarValue> genericVarDef = (StateVarDefinition<IStateVarValue>) stateVar
				.getDefinition();
		StateVar<IStateVarValue> genericVar = new StateVar<>(genericVarDef, stateVar.getValue());
		if (!sanityCheck(genericVar)) {
			throw new IncompatibleVarException(genericVarDef);
		}
		mDiscriminant.add(genericVar);
		mDiscriminantValues.put(genericVarDef, genericVar.getValue());
	}

	private boolean sanityCheck(StateVar<IStateVarValue> stateVar) {
		return mDiscriminantClass.contains(stateVar.getDefinition());
	}

	public IStateVarValue getDiscriminantValue(StateVarDefinition<? extends IStateVarValue> stateVarDef)
			throws VarNotFoundException {
		StateVarDefinition<IStateVarValue> genericVarDef = (StateVarDefinition<IStateVarValue>) stateVarDef;
		if (!mDiscriminantValues.containsKey(genericVarDef)) {
			throw new VarNotFoundException(stateVarDef);
		}
		return mDiscriminantValues.get(genericVarDef);
	}

	public DiscriminantClass getDiscriminantClass() {
		return mDiscriminantClass;
	}

	@Override
	public Iterator<StateVar<IStateVarValue>> iterator() {
		return mDiscriminant.iterator();
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
		return discr.mDiscriminant.equals(mDiscriminant) && discr.mDiscriminantClass.equals(mDiscriminantClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDiscriminant.hashCode();
			result = 31 * result + mDiscriminantClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
