package examples.mobilerobot.factors;

import factors.ICPT;

/**
 * {@link LocationCPT} is a CPT for {@link LocationStateVar}.
 * 
 * @author rsukkerd
 *
 */
public class LocationCPT implements ICPT {

	private static final double SUCC_MOVE_PROB = 1.0;

	public double getConditionalProbability(LocationStateVar locDest, LocationStateVar locSrc, MoveToAction moveTo) {
		if (locDest.getLocation().equals(moveTo.getDestination().getLocation())) {
			return SUCC_MOVE_PROB;
		}
		return 1 - SUCC_MOVE_PROB;
	}
}
