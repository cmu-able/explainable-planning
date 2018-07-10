package mdp;

import java.util.Iterator;

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
public class Discriminant implements IStateVarTuple {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private DiscriminantClass mDiscriminantClass;
	private StateVarTuple mState = new StateVarTuple();

	public Discriminant(DiscriminantClass discriminantClass) {
		mDiscriminantClass = discriminantClass;
	}

	public Discriminant(StateVarDefinition<? extends IStateVarValue>... stateVarDefs) {
		mDiscriminantClass = new DiscriminantClass();
		for (StateVarDefinition<? extends IStateVarValue> varDef : stateVarDefs) {
			mDiscriminantClass.add(varDef);
		}
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		if (!sanityCheck(stateVar)) {
			throw new IncompatibleVarException(stateVar.getDefinition());
		}
		mState.addStateVar(stateVar);
	}

	private boolean sanityCheck(StateVar<? extends IStateVarValue> stateVar) {
		return mDiscriminantClass.contains(stateVar.getDefinition());
	}

	public void addAll(Discriminant discriminant) throws IncompatibleVarException {
		for (StateVar<IStateVarValue> stateVar : discriminant) {
			add(stateVar);
		}
	}

	public DiscriminantClass getDiscriminantClass() {
		return mDiscriminantClass;
	}

	@Override
	public <E extends IStateVarValue> E getStateVarValue(Class<E> valueType, StateVarDefinition<E> stateVarDef)
			throws VarNotFoundException {
		return mState.getStateVarValue(valueType, stateVarDef);
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
		if (!(obj instanceof Discriminant)) {
			return false;
		}
		Discriminant discriminant = (Discriminant) obj;
		return discriminant.mDiscriminantClass.equals(mDiscriminantClass) && discriminant.mState.equals(mState);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDiscriminantClass.hashCode();
			result = 31 * result + mState.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mState.toString();
	}

}
