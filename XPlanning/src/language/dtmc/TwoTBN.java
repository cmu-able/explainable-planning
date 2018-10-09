package language.dtmc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.exceptions.EffectClassNotFoundException;
import language.exceptions.IncompatibleActionException;
import language.exceptions.StateNotFoundException;
import language.mdp.EffectClass;
import language.mdp.ProbabilisticEffect;
import language.mdp.StateVarTuple;

/**
 * {@link TwoTBN} is a 2-step Temporal Bayesian Network (2TBN) for a particular action type (i.e.,
 * {@link ActionDefinition}).
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class TwoTBN<E extends IAction> implements Iterable<Entry<StateVarTuple, E>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDefinition<E> mActionDef;
	private Map<StateVarTuple, Map<EffectClass, ProbabilisticEffect>> m2TBN = new HashMap<>();
	private Map<StateVarTuple, E> mSubPolicy = new HashMap<>();

	public TwoTBN(ActionDefinition<E> actionDef) {
		mActionDef = actionDef;
	}

	/**
	 * Add a probabilistic transition to this 2TBN.
	 * 
	 * @param state
	 *            : A tuple of state variables describing the condition under which the action is taken. This subsumes
	 *            the discriminant variables of the action's effect class.
	 * @param action
	 *            : An action
	 * @param probEffect
	 *            : A probabilistic effect
	 * @throws IncompatibleActionException
	 */
	public void add(StateVarTuple state, E action, ProbabilisticEffect probEffect) throws IncompatibleActionException {
		if (!mActionDef.getActions().contains(action)) {
			throw new IncompatibleActionException(action);
		}
		if (!m2TBN.containsKey(state)) {
			Map<EffectClass, ProbabilisticEffect> probEffects = new HashMap<>();
			m2TBN.put(state, probEffects);
			mSubPolicy.put(state, action);
		}
		m2TBN.get(state).put(probEffect.getEffectClass(), probEffect);
	}

	public ActionDefinition<E> getActionDefinition() {
		return mActionDef;
	}

	/**
	 * Get the action taken in a given state.
	 * 
	 * @param state
	 *            : A tuple of state variables that are sufficient to determine the corresponding action.
	 * @return The action taken in the given state.
	 * @throws StateNotFoundException
	 */
	public E getAction(StateVarTuple state) throws StateNotFoundException {
		if (!mSubPolicy.containsKey(state)) {
			throw new StateNotFoundException(state);
		}
		return mSubPolicy.get(state);
	}

	/**
	 * Get the probabilistic effect of a given effect class, given a previous state.
	 * 
	 * @param state
	 *            : A tuple of state variables that are sufficient to (probabilistically) determine the next state(s).
	 * @param effectClass
	 *            : An effect class
	 * @return The probabilistic effect of the given class.
	 * @throws StateNotFoundException
	 * @throws EffectClassNotFoundException
	 */
	public ProbabilisticEffect getProbabilisticEffect(StateVarTuple state, EffectClass effectClass)
			throws StateNotFoundException, EffectClassNotFoundException {
		if (!m2TBN.containsKey(state)) {
			throw new StateNotFoundException(state);
		}
		if (!m2TBN.get(state).containsKey(effectClass)) {
			throw new EffectClassNotFoundException(effectClass);
		}
		return m2TBN.get(state).get(effectClass);
	}

	@Override
	public Iterator<Entry<StateVarTuple, E>> iterator() {
		return mSubPolicy.entrySet().iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TwoTBN<?>)) {
			return false;
		}
		TwoTBN<?> tbn = (TwoTBN<?>) obj;
		return tbn.mActionDef.equals(mActionDef) && tbn.m2TBN.equals(m2TBN) && tbn.mSubPolicy.equals(mSubPolicy);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mActionDef.hashCode();
			result = 31 * result + m2TBN.hashCode();
			result = 31 * result + mSubPolicy.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
