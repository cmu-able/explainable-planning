package mobilerobot.mapeditor;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

public class Randomizer<E> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private E[] mObjects;
	private EnumeratedIntegerDistribution mDistribution;

	public Randomizer(E[] objects, long seed) {
		double[] probabilities = new double[objects.length];
		Arrays.fill(probabilities, 1.0 / objects.length);
		int[] numsToSample = IntStream.range(0, objects.length).toArray();
		mObjects = objects;
		mDistribution = new EnumeratedIntegerDistribution(numsToSample, probabilities);
		mDistribution.reseedRandomGenerator(seed);
	}

	public Randomizer(E[] objects, double[] probabilities, long seed) {
		int[] numsToSample = IntStream.range(0, objects.length).toArray();
		mObjects = objects;
		mDistribution = new EnumeratedIntegerDistribution(numsToSample, probabilities);
		mDistribution.reseedRandomGenerator(seed);
	}

	public E randomSample() {
		int i = mDistribution.sample();
		return mObjects[i];
	}

	public double getProbabilityOf(E object) {
		int i = Arrays.asList(mObjects).indexOf(object);
		return mDistribution.probability(i);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Randomizer<?>)) {
			return false;
		}
		Randomizer<?> randomizer = (Randomizer<?>) obj;
		return Arrays.equals(randomizer.mObjects, mObjects) && randomizer.mDistribution.equals(mDistribution);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Arrays.hashCode(mObjects);
			result = 31 * result + mDistribution.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
