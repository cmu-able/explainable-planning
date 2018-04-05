package mdp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import exceptions.IncompatibleVarException;
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
	private DiscriminantClass mDiscriminantClass;

	public Discriminant(DiscriminantClass discriminantClass) {
		mDiscriminant = new HashSet<>();
		mDiscriminantClass = discriminantClass;
	}

	public void add(StateVar<? extends IStateVarValue> stateVar) throws IncompatibleVarException {
		StateVarDefinition<IStateVarValue> genericDef = (StateVarDefinition<IStateVarValue>) stateVar.getDefinition();
		StateVar<IStateVarValue> genericStateVar = new StateVar<>(genericDef, stateVar.getValue());
		if (!sanityCheck(genericStateVar)) {
			throw new IncompatibleVarException(genericDef);
		}
		mDiscriminant.add(genericStateVar);
	}

	private boolean sanityCheck(StateVar<IStateVarValue> stateVar) {
		return mDiscriminantClass.contains(stateVar.getDefinition());
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
