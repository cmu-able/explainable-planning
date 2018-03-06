package examples.mobilerobot.factors;

import exceptions.AttributeNameNotFoundException;
import factors.ICPT;

/**
 * {@link BumpedCPT} is a CPT for {@link BumpedStateVar}.
 * 
 * @author rsukkerd
 *
 */
public class BumpedCPT implements ICPT {

	private static final double BUMP_PROB_PARTIALLY_OCCLUDED = 0.2;
	private static final double BUMP_PROB_CLEAR = 0;
	private static final double BUMP_PROB_BLOCKED = 1;

	public double getConditionalProbability(BumpedStateVar rBumpedDest, MoveToAction moveTo, LocationStateVar rLocSrc)
			throws AttributeNameNotFoundException {
		return getConditionalProbabilityHelper(rBumpedDest.getBumped(), moveTo, rLocSrc);
	}

	private double getConditionalProbabilityHelper(RobotBumped bumped, MoveToAction moveTo, LocationStateVar rLocSrc)
			throws AttributeNameNotFoundException {
		if (bumped.isBumped()) {
			if (moveTo.getOcclusion(rLocSrc) == Occlusion.PARTIALLY_OCCLUDED) {
				return BUMP_PROB_PARTIALLY_OCCLUDED;
			}
			if (moveTo.getOcclusion(rLocSrc) == Occlusion.CLEAR) {
				return BUMP_PROB_CLEAR;
			}
			return BUMP_PROB_BLOCKED;
		}
		return 1 - getConditionalProbabilityHelper(new RobotBumped(!bumped.isBumped()), moveTo, rLocSrc);
	}
}
