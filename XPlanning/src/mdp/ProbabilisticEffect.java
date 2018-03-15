package mdp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import factors.IStateVarValue;
import factors.StateVar;

/**
 * {@link ProbabilisticEffect} is a distribution over the changed state variables as a result of an action.
 * 
 * @author rsukkerd
 *
 */
public class ProbabilisticEffect implements Iterable<Entry<Set<StateVar<IStateVarValue>>, Double>> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<Set<StateVar<IStateVarValue>>, Double> mProbEffect;

	public ProbabilisticEffect() {
		mProbEffect = new HashMap<>();
	}

	public void put(Set<StateVar<IStateVarValue>> effect, Double prob) {
		mProbEffect.put(effect, prob);
	}

	@Override
	public Iterator<Entry<Set<StateVar<IStateVarValue>>, Double>> iterator() {
		return mProbEffect.entrySet().iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ProbabilisticEffect)) {
			return false;
		}
		ProbabilisticEffect probEffect = (ProbabilisticEffect) obj;
		return probEffect.mProbEffect.equals(mProbEffect);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mProbEffect.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
