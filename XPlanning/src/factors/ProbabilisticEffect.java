package factors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ProbabilisticEffect implements Iterable<Entry<Set<StateVar<IStateVarValue>>, Double>> {

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
}
