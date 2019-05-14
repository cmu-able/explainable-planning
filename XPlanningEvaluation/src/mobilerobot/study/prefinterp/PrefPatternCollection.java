package mobilerobot.study.prefinterp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.collections4.iterators.PermutationIterator;

public class PrefPatternCollection implements Iterable<WADDPattern> {

	private List<WADDPattern> mPrefPatterns = new ArrayList<>();
	private Random mRandom = new Random(0L);

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

	public WADDPattern getRandomPrefPattern() {
		int i = mRandom.nextInt(mPrefPatterns.size());
		return mPrefPatterns.get(i);
	}

	@Override
	public Iterator<WADDPattern> iterator() {
		return mPrefPatterns.iterator();
	}

}
