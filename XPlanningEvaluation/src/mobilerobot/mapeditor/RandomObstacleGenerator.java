package mobilerobot.mapeditor;

import java.util.Iterator;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;

import examples.mobilerobot.dsm.Connection;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.models.Occlusion;

public class RandomObstacleGenerator {

	private static final int[] OCC_INDICES = new int[] { 0, 1, 2 };
	private static final Occlusion[] INDEXED_OCC = new Occlusion[] { Occlusion.CLEAR, Occlusion.PARTIALLY_OCCLUDED,
			Occlusion.BLOCKED };

	private EnumeratedIntegerDistribution mDistribution;

	public RandomObstacleGenerator(double[] occlusionProbabilities) {
		mDistribution = new EnumeratedIntegerDistribution(OCC_INDICES, occlusionProbabilities);
	}

	public void randomlyPlaceObstacles(MapTopology mapTopology) {
		Iterator<Connection> connIter = mapTopology.connectionIterator();
		while (connIter.hasNext()) {
			Connection connection = connIter.next();
			int i = mDistribution.sample();
			Occlusion randomOcclusion = INDEXED_OCC[i];
			connection.putConnectionAttribute("occlusion", randomOcclusion);
		}
	}
}
