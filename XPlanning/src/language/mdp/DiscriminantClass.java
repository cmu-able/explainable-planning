package language.mdp;

import java.util.Iterator;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;

/**
 * {@link DiscriminantClass} is a set of {@link StateVarDefinition} that defines a class of {@link Discriminant}s.
 * 
 * Assumption: A discriminant class is either a class of independent variables, or a class of all-dependent variables,
 * w.r.t. action precondition.
 * 
 * @author rsukkerd
 *
 */
public class DiscriminantClass implements IStateVarClass {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarClass mStateVarClass = new StateVarClass();

	public DiscriminantClass() {
		// mDiscriminantClass initially empty
	}

	public void add(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		mStateVarClass.add(stateVarDef);
	}

	public void addAll(DiscriminantClass discriminantClass) {
		mStateVarClass.addAll(discriminantClass.mStateVarClass);
	}

	public boolean contains(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		return mStateVarClass.contains(stateVarDef);
	}

	public boolean isEmpty() {
		return mStateVarClass.isEmpty();
	}

	public StateVarClass getStateVarClass() {
		return mStateVarClass;
	}

	@Override
	public Iterator<StateVarDefinition<IStateVarValue>> iterator() {
		return mStateVarClass.iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DiscriminantClass)) {
			return false;
		}
		DiscriminantClass discrClass = (DiscriminantClass) obj;
		return discrClass.mStateVarClass.equals(mStateVarClass);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStateVarClass.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
