package mobilerobot.mapeditor;

import java.util.Iterator;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.IEdgeAttribute;
import examples.mobilerobot.dsm.MapTopology;

public class RandomEdgeAttributeGenerator<E extends IEdgeAttribute> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mEdgeAttrName;
	private Randomizer<E> mEdgeAttrRandomizer;

	public RandomEdgeAttributeGenerator(String edgeAttrName, E[] edgeAttrValues, long seed) {
		mEdgeAttrName = edgeAttrName;
		mEdgeAttrRandomizer = new Randomizer<>(edgeAttrValues, seed);
	}

	public RandomEdgeAttributeGenerator(String edgeAttrName, E[] edgeAttrValues, double[] edgeAttrProbs, long seed) {
		mEdgeAttrName = edgeAttrName;
		mEdgeAttrRandomizer = new Randomizer<>(edgeAttrValues, edgeAttrProbs, seed);
	}

	public void randomlyAssignEdgeAttributeValues(MapTopology mapTopology) {
		Iterator<Connection> edgeIter = mapTopology.connectionIterator();
		while (edgeIter.hasNext()) {
			Connection connection = edgeIter.next();
			E randomEdgeAttrValue = mEdgeAttrRandomizer.randomSample();
			connection.putConnectionAttribute(mEdgeAttrName, randomEdgeAttrValue);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RandomEdgeAttributeGenerator<?>)) {
			return false;
		}
		RandomEdgeAttributeGenerator<?> generator = (RandomEdgeAttributeGenerator<?>) obj;
		return generator.mEdgeAttrName.equals(mEdgeAttrName)
				&& generator.mEdgeAttrRandomizer.equals(mEdgeAttrRandomizer);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mEdgeAttrName.hashCode();
			result = 31 * result + mEdgeAttrRandomizer.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
