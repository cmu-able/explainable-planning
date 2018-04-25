package dtmc;

import java.util.HashMap;
import java.util.Map;

import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.PredicateNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import mdp.EffectClass;
import mdp.ProbabilisticEffect;
import policy.Predicate;

public class TwoTBN<E extends IAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDefinition<E> mActionDef;
	private Map<Predicate, Map<EffectClass, ProbabilisticEffect>> m2TBN = new HashMap<>();
	private Map<Predicate, E> mSubPolicy = new HashMap<>();

	public TwoTBN(ActionDefinition<E> actionDef) {
		mActionDef = actionDef;
	}

	public void add(Predicate predicate, E action, ProbabilisticEffect probEffect) throws IncompatibleActionException {
		if (!mActionDef.getActions().contains(action)) {
			throw new IncompatibleActionException(action);
		}
		if (!m2TBN.containsKey(predicate)) {
			Map<EffectClass, ProbabilisticEffect> probEffects = new HashMap<>();
			m2TBN.put(predicate, probEffects);
			mSubPolicy.put(predicate, action);
		}
		m2TBN.get(predicate).put(probEffect.getEffectClass(), probEffect);
	}

	public E getAction(Predicate predicate) throws PredicateNotFoundException {
		if (!mSubPolicy.containsKey(predicate)) {
			throw new PredicateNotFoundException(predicate);
		}
		return mSubPolicy.get(predicate);
	}

	public ProbabilisticEffect getProbabilisticEffect(Predicate predicate, EffectClass effectClass)
			throws PredicateNotFoundException, EffectClassNotFoundException {
		if (!m2TBN.containsKey(predicate)) {
			throw new PredicateNotFoundException(predicate);
		}
		if (!m2TBN.get(predicate).containsKey(effectClass)) {
			throw new EffectClassNotFoundException(effectClass);
		}
		return m2TBN.get(predicate).get(effectClass);
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
