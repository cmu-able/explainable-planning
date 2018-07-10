package dtmc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.StateNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import mdp.EffectClass;
import mdp.ProbabilisticEffect;
import mdp.StatePredicate;

/**
 * {@link TwoTBN} is a 2-step Temporal Bayesian Network (2TBN) for a particular action type (i.e.,
 * {@link ActionDefinition}).
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class TwoTBN<E extends IAction> implements Iterable<Entry<StatePredicate, E>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDefinition<E> mActionDef;
	private Map<StatePredicate, Map<EffectClass, ProbabilisticEffect>> m2TBN = new HashMap<>();
	private Map<StatePredicate, E> mSubPolicy = new HashMap<>();

	public TwoTBN(ActionDefinition<E> actionDef) {
		mActionDef = actionDef;
	}

	public void add(StatePredicate state, E action, ProbabilisticEffect probEffect) throws IncompatibleActionException {
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

	public E getAction(StatePredicate state) throws StateNotFoundException {
		if (!mSubPolicy.containsKey(state)) {
			throw new StateNotFoundException(state);
		}
		return mSubPolicy.get(state);
	}

	public ProbabilisticEffect getProbabilisticEffect(StatePredicate state, EffectClass effectClass)
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
	public Iterator<Entry<StatePredicate, E>> iterator() {
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
