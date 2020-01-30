package language.mdp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.ActionNotFoundException;
import language.exceptions.IncompatibleActionException;
import language.exceptions.StateVarClassNotFoundException;

/**
 * {@link Precondition} defines a precondition for each action in a particular {@link ActionDefinition}. Precondition is
 * in a CNF, where each clause is a {@link UnivarPredicate} of a unique state variable. If a state variable is not
 * present in the precondition, it means that there is no restriction on the applicable values of that variable.
 * 
 * Note: Since {@link Precondition} is a CNF of allowable values of each state variable, the precondition of a composite
 * {@link ActionDefinition} needs to be NO stronger than the precondition of any individual action definition. This is
 * OK because the precondition of a composite action is only used in the reward structure. The individual actions that
 * make up the composite action in the reward structure are synchronized with the actions in the modules, which have
 * stronger (or equivalent) precondition.
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

	// Univariate predicates
	private Map<E, Map<StateVarDefinition<? extends IStateVarValue>, UnivarPredicate<? extends IStateVarValue>>> mUnivarPredicates = new HashMap<>();

	// Multivariate predicates
	private Map<E, Map<StateVarClass, MultivarPredicate>> mMultivarPredicates = new HashMap<>();
	private Set<StateVarClass> mMultivarClasses = new HashSet<>(); // for fast look-up

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

	/**
	 * Add an allowable value tuple of a class of state variables.
	 * 
	 * @param action
	 * @param stateVarClass
	 * @param allowableTuple
	 * @throws IncompatibleActionException
	 */
	public void add(E action, StateVarClass stateVarClass, StateVarTuple allowableTuple)
			throws IncompatibleActionException {
		if (!sanityCheck(action)) {
			throw new IncompatibleActionException(action);
		}

		// Keep track of variable classes of multivariate predicates for fast look-up
		mMultivarClasses.add(stateVarClass);

		if (!mMultivarPredicates.containsKey(action)) {
			Map<StateVarClass, MultivarPredicate> predicates = new HashMap<>();
			// Create a new multivariate predicate for a new class of variables, and add a first allowable value tuple
			MultivarPredicate predicate = new MultivarPredicate(stateVarClass);
			predicate.addAllowableTuple(allowableTuple);
			predicates.put(stateVarClass, predicate);
			mMultivarPredicates.put(action, predicates);
		} else if (!mMultivarPredicates.get(action).containsKey(stateVarClass)) {
			// Create a new multivariate predicate for a new class of variables, and add a first allowable value tuple
			MultivarPredicate predicate = new MultivarPredicate(stateVarClass);
			predicate.addAllowableTuple(allowableTuple);
			mMultivarPredicates.get(action).put(stateVarClass, predicate);
		} else {
			// Add a new allowable value tuple to an existing multivariate predicate
			MultivarPredicate predicate = mMultivarPredicates.get(action).get(stateVarClass);
			predicate.addAllowableTuple(allowableTuple);
		}
	}

	/**
	 * This method must be invoked before getApplicableTuples().
	 * 
	 * @param stateVarClass
	 *            : Class of state variables
	 * @return Whether this precondition has a multivariate predicate on the class of state variables.
	 */
	public boolean hasMultivarPredicateOn(StateVarClass stateVarClass) {
		return mMultivarClasses.contains(stateVarClass);
	}

	/**
	 * This method must be invoked before getPartialApplicableTuples().
	 * 
	 * @param stateVarClass
	 *            : Class of state variables
	 * @return Whether this precondition has a multivariate predicate partially on the class of state variables.
	 */
	public boolean hasMultivarPredicatePartiallyOn(StateVarClass stateVarClass) {
		for (StateVarClass multivarClass : mMultivarClasses) {
			if (multivarClass.containsAll(stateVarClass)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get a set of applicable values of a given state variable for a given action. The state variable has a univariate
	 * predicate on it.
	 * 
	 * @param action
	 *            : Action
	 * @param stateVarDef
	 *            : State variable
	 * @return A set of applicable values of the state variable for the action
	 * @throws ActionNotFoundException
	 */
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

	/**
	 * Get a set of applicable value tuples of a given class of state variables for a given action. The state variables
	 * have a multivariate predicate on them.
	 * 
	 * @param action
	 *            : Action
	 * @param stateVarClass
	 *            : Class of state variables
	 * @return A set of applicable value tuples of the state variables for the action
	 * @throws ActionNotFoundException
	 * @throws StateVarClassNotFoundException
	 */
	public Set<StateVarTuple> getApplicableTuples(E action, StateVarClass stateVarClass)
			throws ActionNotFoundException, StateVarClassNotFoundException {
		if (!sanityCheck(action)) {
			throw new ActionNotFoundException(action);
		}
		if (!mMultivarPredicates.containsKey(action) || !mMultivarPredicates.get(action).containsKey(stateVarClass)) {
			throw new StateVarClassNotFoundException(stateVarClass);
		}
		return mMultivarPredicates.get(action).get(stateVarClass).getAllowableTuples();
	}

	/**
	 * Get a set of applicable value tuples of a given class of state variables for a given action, given a constraining
	 * tuple. The state variables have a multivariate predicate on them.
	 * 
	 * @param action
	 *            : Action
	 * @param stateVarClass
	 *            : Class of state variables
	 * @param constrainingTuple
	 *            : Value tuple that must be contained in all of the result tuples
	 * @return A set of applicable value tuples of the state variables for the action, given the constraining tuple
	 * @throws StateVarClassNotFoundException
	 * @throws ActionNotFoundException
	 */
	public Set<StateVarTuple> getApplicableTuples(E action, StateVarClass stateVarClass,
			StateVarTuple constrainingTuple) throws ActionNotFoundException, StateVarClassNotFoundException {
		Set<StateVarTuple> constrainedApplicableTuples = new HashSet<>();
		Set<StateVarTuple> unconstrainedApplicableTuples = getApplicableTuples(action, stateVarClass);
		for (StateVarTuple tuple : unconstrainedApplicableTuples) {
			if (tuple.contains(constrainingTuple)) {
				constrainedApplicableTuples.add(tuple);
			}
		}
		return constrainedApplicableTuples;
	}

	/**
	 * Get a set of partial applicable value tuples of a given class of state variables for a given action. The tuples
	 * are partial because the action's applicability depends on other variables outside the given class.
	 * 
	 * @param action
	 *            : Action
	 * @param stateVarClass
	 *            : Class of state variables
	 * @return A set of partial applicable value tuples of the state variables for the action
	 */
	public Set<StateVarTuple> getPartialApplicableTuples(E action, StateVarClass stateVarClass) {
		Set<StateVarTuple> partialApplicableTuples = new HashSet<>();
		for (StateVarClass multivarClass : mMultivarClasses) {
			if (multivarClass.containsAll(stateVarClass)) {
				Set<StateVarTuple> allowableTuples = mMultivarPredicates.get(action).get(multivarClass)
						.getAllowableTuples();

				for (StateVarTuple allowableTuple : allowableTuples) {
					StateVarTuple partialApplicableTuple = filterStateVarTuple(allowableTuple, stateVarClass);
					partialApplicableTuples.add(partialApplicableTuple);
				}
			}
		}
		return partialApplicableTuples;
	}

	/**
	 * Filter a state variable tuple to contain only a given set of variables.
	 * 
	 * @param stateVarTuple
	 *            : State variable tuple to be filtered
	 * @param filterStateVarClass
	 *            : Class of state variables to be preserved in the result
	 * @return Filtered state variable tuple
	 */
	private StateVarTuple filterStateVarTuple(StateVarTuple stateVarTuple, StateVarClass filterStateVarClass) {
		StateVarTuple filteredTuple = new StateVarTuple();
		for (StateVar<IStateVarValue> stateVar : stateVarTuple) {
			if (filterStateVarClass.contains(stateVar.getDefinition())) {
				filteredTuple.addStateVar(stateVar);
			}
		}
		return filteredTuple;
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
		return precond.mActionDef.equals(mActionDef) && precond.mUnivarPredicates.equals(mUnivarPredicates)
				&& precond.mMultivarPredicates.equals(mMultivarPredicates);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDef.hashCode();
			result = 31 * result + mUnivarPredicates.hashCode();
			result = 31 * result + mMultivarPredicates.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
