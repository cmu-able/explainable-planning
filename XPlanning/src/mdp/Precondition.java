package mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import exceptions.ActionNotFoundException;
import exceptions.IncompatibleActionException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;

/**
 * {@link Precondition} defines a precondition for each action in a particular {@link ActionDefinition}. If a state
 * variable is not present in the precondition, it means that there is no restriction on the applicable values of that
 * variable.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class Precondition<E extends IAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDefinition<E> mActionDef;
	private Map<E, Map<StateVarDefinition<? extends IStateVarValue>, Set<IStateVarValue>>> mActionPreconds = new HashMap<>();

	public Precondition(ActionDefinition<E> actionDef) {
		mActionDef = actionDef;
	}

	public <T extends IStateVarValue> void add(E action, StateVarDefinition<T> stateVarDef, T value)
			throws IncompatibleActionException {
		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}
		if (!mActionPreconds.containsKey(action)) {
			Map<StateVarDefinition<? extends IStateVarValue>, Set<IStateVarValue>> preCond = new HashMap<>();
			Set<IStateVarValue> applicableValues = new HashSet<>();
			applicableValues.add(value);
			preCond.put(stateVarDef, applicableValues);
			mActionPreconds.put(action, preCond);
		} else {
			mActionPreconds.get(action).get(stateVarDef).add(value);
		}
	}

	public <T extends IStateVarValue> Set<T> getApplicableValues(E action, StateVarDefinition<T> stateVarDef)
			throws ActionNotFoundException {
		if (!sanityCheck(action)) {
			throw new ActionNotFoundException(action);
		}
		if (!mActionPreconds.containsKey(action) || !mActionPreconds.get(action).containsKey(stateVarDef)) {
			stateVarDef.getPossibleValues();
		}
		return (Set<T>) mActionPreconds.get(action).get(stateVarDef);
	}

	public boolean isActionApplicable(E action, IPredicate predicate) throws ActionNotFoundException {
		if (!sanityCheck(action)) {
			throw new ActionNotFoundException(action);
		}
		Map<StateVarDefinition<? extends IStateVarValue>, Set<IStateVarValue>> actionPrecond = mActionPreconds
				.get(action);
		for (StateVar<IStateVarValue> stateVar : predicate) {
			StateVarDefinition<IStateVarValue> varDef = stateVar.getDefinition();
			IStateVarValue value = stateVar.getValue();
			if (actionPrecond.containsKey(varDef) && !actionPrecond.get(varDef).contains(value)) {
				return false;
			}
		}
		return true;
	}

	private boolean sanityCheck(E action) {
		return mActionDef.getActions().contains(action);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Precondition<?>)) {
			return false;
		}
		Precondition<?> precond = (Precondition<?>) obj;
		return precond.mActionDef.equals(mActionDef) && precond.mActionPreconds.equals(mActionPreconds);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDef.hashCode();
			result = 31 * result + mActionPreconds.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
