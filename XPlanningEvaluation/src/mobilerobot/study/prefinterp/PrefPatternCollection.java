package mobilerobot.study.prefinterp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.iterators.PermutationIterator;

public class PrefPatternCollection implements Iterable<WADDPattern> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private List<WADDPattern> mPrefPatterns = new ArrayList<>();

	public PrefPatternCollection(List<String> objectiveNames, List<List<Double>> paramLists) {
		populatePrefPatterns(objectiveNames, paramLists);
	}

	private void populatePrefPatterns(List<String> objectiveNames, List<List<Double>> paramLists) {
		for (List<Double> paramList : paramLists) {
			PermutationIterator<Double> permIter = new PermutationIterator<>(paramList);
			List<WADDPattern> waddPatterns = createWADDPatterns(objectiveNames, permIter);
			mPrefPatterns.addAll(waddPatterns);
		}
	}

	private List<WADDPattern> createWADDPatterns(List<String> objectiveNames, PermutationIterator<Double> permIter) {
		List<WADDPattern> waddPatterns = new ArrayList<>();
		while (permIter.hasNext()) {
			List<Double> perm = permIter.next();
			WADDPattern waddPattern = new WADDPattern();
			for (int i = 0; i < objectiveNames.size(); i++) {
				String objectiveName = objectiveNames.get(i);
				double weight = perm.get(i);
				waddPattern.putWeight(objectiveName, weight);
			}
			waddPatterns.add(waddPattern);
		}
		return waddPatterns;
	}

	@Override
	public Iterator<WADDPattern> iterator() {
		return mPrefPatterns.iterator();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PrefPatternCollection)) {
			return false;
		}
		PrefPatternCollection coll = (PrefPatternCollection) obj;
		return coll.mPrefPatterns.equals(mPrefPatterns);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPrefPatterns.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
