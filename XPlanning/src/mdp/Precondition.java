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
	public <T extends IStateVarValue> void add(E action, StateVarDefinition<T> stateVarDef, T value)
			throws IncompatibleActionException {
		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}
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

	public void add(E action, MultivarClass multivarClass, IStatePredicate statePredicate)
			throws IncompatibleActionException {
		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}
		if (!mMultivarPredicates.containsKey(action)) {
			Map<MultivarClass, MultivarPredicate> predicates = new HashMap<>();
			MultivarPredicate predicate = new MultivarPredicate(multivarClass);
			// TODO
		}
	}

	/**
	 * For univariate precondition: Add an applicable value of a state variable, for each action.
	 * 
	 * @param action
	 * @param stateVarDef
	 * @param value
	 * @throws IncompatibleActionException
	 */
	public <T extends IStateVarValue> void add2(E action, StateVarDefinition<T> stateVarDef, T value)
			throws IncompatibleActionException {
		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}
		if (!mUnivarPreconds.containsKey(action)) {
			Map<StateVarDefinition<? extends IStateVarValue>, Set<IStateVarValue>> precond = new HashMap<>();
			Set<IStateVarValue> applicableUnivarValues = new HashSet<>();
			applicableUnivarValues.add(value);
			precond.put(stateVarDef, applicableUnivarValues);
			mUnivarPreconds.put(action, precond);
		} else {
			mUnivarPreconds.get(action).get(stateVarDef).add(value);
		}
	}

	/**
	 * For multivariate precondition: Add a set of (dependent) applicable values of different state variables, for each
	 * action.
	 * 
	 * @param action
	 * @param stateVars
	 * @throws IncompatibleActionException
	 */
	public void add(E action, StateVar<? extends IStateVarValue>... stateVars) throws IncompatibleActionException {
		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}

		// Multivariate condition
		Set<StateVar<? extends IStateVarValue>> multivarCond = new HashSet<>();
		for (StateVar<? extends IStateVarValue> var : stateVars) {
			multivarCond.add(var);
		}

		if (!mMultivarPreconds.containsKey(action)) {
			Set<Set<StateVar<? extends IStateVarValue>>> applicableMultivarValues = new HashSet<>();
			applicableMultivarValues.add(multivarCond);
			mMultivarPreconds.put(action, applicableMultivarValues);
		} else {
			mMultivarPreconds.get(action).add(multivarCond);
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
