package mdp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import exceptions.ActionNotFoundException;
import exceptions.IncompatibleActionException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVarDefinition;

/**
 * {@link Precondition} defines a precondition for each action in a particular {@link ActionDefinition}. Precondition is
 * in a CNF, where each clause is a {@link UnivarPredicate} of a unique state variable. If a state variable is not
 * present in the precondition, it means that there is no restriction on the applicable values of that variable.
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

	private Map<E, Map<StateVarDefinition<? extends IStateVarValue>, UnivarPredicate<? extends IStateVarValue>>> mUnivarPredicates = new HashMap<>();

	public Precondition(ActionDefinition<E> actionDef) {
		mActionDef = actionDef;
	}

	/**
	 * Add an allowable value of a single state variable.
	 * 
	 * @param action
	 * @param stateVar
	 * @throws IncompatibleActionException
	 */
	public <T extends IStateVarValue> void add(E action, StateVarDefinition<T> stateVarDef, T allowableValue)
			throws IncompatibleActionException {
		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}

		if (!mUnivarPredicates.containsKey(action)) {
			Map<StateVarDefinition<? extends IStateVarValue>, UnivarPredicate<? extends IStateVarValue>> predicates = new HashMap<>();
			// Create a new univariate predicate for a new variable, and add a first allowable value
			UnivarPredicate<T> predicate = new UnivarPredicate<>(stateVarDef);
			predicate.addAllowableValue(allowableValue);
			predicates.put(stateVarDef, predicate);
			mUnivarPredicates.put(action, predicates);
		} else if (!mUnivarPredicates.get(action).containsKey(stateVarDef)) {
			// Create a new univariate predicate for a new variable, and add a first allowable value
			UnivarPredicate<T> predicate = new UnivarPredicate<>(stateVarDef);
			predicate.addAllowableValue(allowableValue);
			mUnivarPredicates.get(action).put(stateVarDef, predicate);
		} else {
			// Add a new allowable value to an existing univariate predicate
			// Casting: state variable of type T always maps to univariate predicate of type T
			UnivarPredicate<T> predicate = (UnivarPredicate<T>) mUnivarPredicates.get(action).get(stateVarDef);
			predicate.addAllowableValue(allowableValue);
		}
	}

	public <T extends IStateVarValue> Set<T> getApplicableValues(E action, StateVarDefinition<T> stateVarDef)
			throws ActionNotFoundException {
		if (!sanityCheck(action)) {
			throw new ActionNotFoundException(action);
		}
		if (!mUnivarPredicates.containsKey(action) || !mUnivarPredicates.get(action).containsKey(stateVarDef)) {
			return stateVarDef.getPossibleValues();
		}
		// Casting: state variable of type T always maps to univariate predicate of type T
		return (Set<T>) mUnivarPredicates.get(action).get(stateVarDef).getAllowableValues();
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
		return precond.mActionDef.equals(mActionDef) && precond.mUnivarPredicates.equals(mUnivarPredicates);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDef.hashCode();
			result = 31 * result + mUnivarPredicates.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
