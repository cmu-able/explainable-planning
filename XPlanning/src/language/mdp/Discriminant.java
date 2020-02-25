package language.mdp;

import java.util.Iterator;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.IncompatibleVarException;
import language.exceptions.IncompatibleVarsException;
import language.exceptions.VarNotFoundException;

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

	public void addAllRelevant(StateVarTuple stateVarTuple) throws VarNotFoundException {
		for (StateVarDefinition<IStateVarValue> discrVarDef : mDiscriminantClass) {
			IStateVarValue value = stateVarTuple.getStateVarValue(IStateVarValue.class, discrVarDef);
			StateVar<IStateVarValue> stateVar = discrVarDef.getStateVar(value);
			mVarTuple.addStateVar(stateVar);
		}
	}

	private boolean sanityCheck(StateVar<? extends IStateVarValue> stateVar) {
		return mDiscriminantClass.contains(stateVar.getDefinition());
	}

	private boolean sanityCheck(IStateVarTuple stateVarTuple) {
		for (StateVar<? extends IStateVarValue> stateVar : stateVarTuple) {
			if (!sanityCheck(stateVar)) {
				return false;
			}
		}
		return true;
	}

	public DiscriminantClass getDiscriminantClass() {
		return mDiscriminantClass;
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
