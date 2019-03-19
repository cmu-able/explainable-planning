package mobilerobot.mapeditor;

import java.util.Iterator;

import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;

public class RandomNodeAttributeGenerator<E extends INodeAttribute> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mNodeAttrName;
	private Randomizer<E> mNodeAttrRandomizer;

	public RandomNodeAttributeGenerator(String nodeAttrName, E[] nodeAttrValues) {
		mNodeAttrName = nodeAttrName;
		mNodeAttrRandomizer = new Randomizer<>(nodeAttrValues);
	}

	public RandomNodeAttributeGenerator(String nodeAttrName, E[] nodeAttrValues, double[] nodeAttrProbs) {
		mNodeAttrName = nodeAttrName;
		mNodeAttrRandomizer = new Randomizer<>(nodeAttrValues, nodeAttrProbs);
	}

	public void randomlyAssignNodeAttributeValues(MapTopology mapTopology) {
		Iterator<LocationNode> nodeIter = mapTopology.nodeIterator();
		while (nodeIter.hasNext()) {
			LocationNode locNode = nodeIter.next();
			E randomNodeAttrValue = mNodeAttrRandomizer.randomSample();
			locNode.putNodeAttribute(mNodeAttrName, randomNodeAttrValue);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RandomNodeAttributeGenerator<?>)) {
			return false;
		}
		RandomNodeAttributeGenerator<?> generator = (RandomNodeAttributeGenerator<?>) obj;
		return generator.mNodeAttrName.equals(mNodeAttrName)
				&& generator.mNodeAttrRandomizer.equals(mNodeAttrRandomizer);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNodeAttrName.hashCode();
			result = 31 * result + mNodeAttrRandomizer.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
