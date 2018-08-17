package language.mdp;

import java.util.Iterator;

import language.exceptions.IncompatibleVarException;
import language.exceptions.IncompatibleVarsException;
import language.exceptions.VarNotFoundException;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;
import language.qfactors.StateVarDefinition;

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
	private StateVarTuple mVarTuple = new StateVarTuple();

	public Discriminant(DiscriminantClass discriminantClass) {
		mDiscriminantClass = discriminantClass;
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		if (!sanityCheck(stateVar)) {
			throw new IncompatibleVarException(stateVar.getDefinition());
		}
		mVarTuple.addStateVar(stateVar);
	}

	public void addAll(Discriminant discriminant) throws IncompatibleVarsException {
		if (!sanityCheck(discriminant)) {
			throw new IncompatibleVarsException(discriminant);
		}
		mVarTuple.addStateVarTuple(discriminant.mVarTuple);
	}

	private boolean sanityCheck(StateVar<? extends IStateVarValue> stateVar) {
		return mDiscriminantClass.contains(stateVar.getDefinition());
	}

	private boolean sanityCheck(Discriminant discriminant) {
		return discriminant.getDiscriminantClass().equals(mDiscriminantClass);
	}

	public DiscriminantClass getDiscriminantClass() {
		return mDiscriminantClass;
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
		if (!(obj instanceof Discriminant)) {
			return false;
		}
		Discriminant discriminant = (Discriminant) obj;
		return discriminant.mDiscriminantClass.equals(mDiscriminantClass) && discriminant.mVarTuple.equals(mVarTuple);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDiscriminantClass.hashCode();
			result = 31 * result + mVarTuple.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mVarTuple.toString();
	}

}
