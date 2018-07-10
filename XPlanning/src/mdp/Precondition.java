package mdp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import exceptions.ActionNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleVarsException;
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

	// Univariate preconditions
	private Map<E, Map<StateVarDefinition<? extends IStateVarValue>, Set<IStateVarValue>>> mUnivarPreconds = new HashMap<>();
	private Map<E, Map<StateVarDefinition<? extends IStateVarValue>, UnivarPredicate<? extends IStateVarValue>>> mUnivarPredicates = new HashMap<>();

	// Multivariate preconditions
	private Map<E, Set<Set<StateVar<? extends IStateVarValue>>>> mMultivarPreconds = new HashMap<>();
	private Map<E, Map<MultivarClass, MultivarPredicate>> mMultivarPredicates = new HashMap<>();

	public Precondition(ActionDefinition<E> actionDef) {
		mActionDef = actionDef;
	}

	/**
	 * For univariate predicate: Add an allowable value of a single state variable.
	 * 
	 * @param action
	 * @param stateVar
	 * @throws IncompatibleActionException
	 */
	public <T extends IStateVarValue> void add(E action, StateVar<T> stateVar) throws IncompatibleActionException {
		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}

		StateVarDefinition<T> stateVarDef = stateVar.getDefinition();
		T value = stateVar.getValue();

		if (!mUnivarPredicates.containsKey(action)) {
			Map<StateVarDefinition<? extends IStateVarValue>, UnivarPredicate<? extends IStateVarValue>> predicates = new HashMap<>();
			// Create a new univariate predicate for a new variable, and add a first allowable value
			UnivarPredicate<T> predicate = new UnivarPredicate<>(stateVarDef);
			predicate.addAllowableValue(value);
			predicates.put(stateVarDef, predicate);
			mUnivarPredicates.put(action, predicates);
		} else if (!mUnivarPredicates.get(action).containsKey(stateVarDef)) {
			// Create a new univariate predicate for a new variable, and add a first allowable value
			UnivarPredicate<T> predicate = new UnivarPredicate<>(stateVarDef);
			predicate.addAllowableValue(value);
			mUnivarPredicates.get(action).put(stateVarDef, predicate);
		} else {
			// Add a new allowable value to an existing univariate predicate
			UnivarPredicate<T> predicate = (UnivarPredicate<T>) mUnivarPredicates.get(action).get(stateVarDef);
			predicate.addAllowableValue(value);
		}
	}

	/**
	 * For multivariate predicate: Add an allowable value tuple of a set of state variables.
	 * 
	 * @param action
	 * @param statePredicate
	 * @throws IncompatibleActionException
	 * @throws IncompatibleVarsException
	 */
	public void add(E action, StatePredicate statePredicate)
			throws IncompatibleActionException, IncompatibleVarsException {
		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}

		MultivarClass multivarClass = statePredicate.getMultivarClass();

		if (!mMultivarPredicates.containsKey(action)) {
			Map<MultivarClass, MultivarPredicate> predicates = new HashMap<>();
			// Create a new multivariate predicate for a new set of variables, and add a first allowable value tuple
			MultivarPredicate predicate = new MultivarPredicate(multivarClass);
			predicate.addAllowableValueTuple(statePredicate);
			predicates.put(multivarClass, predicate);
			mMultivarPredicates.put(action, predicates);
		} else if (!mMultivarPredicates.get(action).containsKey(multivarClass)) {
			// Create a new multivariate predicate for a new set of variables, and add a first allowable value tuple
			MultivarPredicate predicate = new MultivarPredicate(multivarClass);
			predicate.addAllowableValueTuple(statePredicate);
			mMultivarPredicates.get(action).put(multivarClass, predicate);
		} else {
			// Add a new allowable value tuple to an existing multivariate predicate
			MultivarPredicate predicate = mMultivarPredicates.get(action).get(multivarClass);
			predicate.addAllowableValueTuple(statePredicate);
		}
	}

	public <T extends IStateVarValue> Set<T> getApplicableValues(E action, StateVarDefinition<T> stateVarDef)
			throws ActionNotFoundException {
		if (!sanityCheck(action)) {
			throw new ActionNotFoundException(action);
		}
		if (!mUnivarPreconds.containsKey(action) || !mUnivarPreconds.get(action).containsKey(stateVarDef)) {
			return stateVarDef.getPossibleValues();
		}
		return (Set<T>) mUnivarPreconds.get(action).get(stateVarDef);
	}

	public boolean isActionApplicable(E action, IStatePredicate predicate) throws ActionNotFoundException {
		if (!sanityCheck(action)) {
			throw new ActionNotFoundException(action);
		}
		Map<StateVarDefinition<? extends IStateVarValue>, Set<IStateVarValue>> actionPrecond = mUnivarPreconds
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
		return precond.mActionDef.equals(mActionDef) && precond.mUnivarPreconds.equals(mUnivarPreconds);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDef.hashCode();
			result = 31 * result + mUnivarPreconds.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
