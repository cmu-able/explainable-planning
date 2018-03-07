package examples.mobilerobot.factors;

import exceptions.AttributeNameNotFoundException;
import factors.ICPT;
import factors.StateVar;

/**
 * {@link BumpedCPT} is a CPT for a state variable of type {@link RobotBumped}.
 * 
 * @author rsukkerd
 *
 */
public class BumpedCPT implements ICPT {

	private static final double BUMP_PROB_PARTIALLY_OCCLUDED = 0.2;
	private static final double BUMP_PROB_CLEAR = 0;
	private static final double BUMP_PROB_BLOCKED = 1;

	public double getConditionalProbability(StateVar<RobotBumped> rBumpedDest, StateVar<Location> rLocSrc,
			MoveToAction moveTo) throws AttributeNameNotFoundException {
		return getConditionalProbabilityHelper(rBumpedDest.getValue().hasBumped(), rLocSrc, moveTo);
	}

	private double getConditionalProbabilityHelper(boolean bumped, StateVar<Location> rLocSrc, MoveToAction moveTo)
			throws AttributeNameNotFoundException {
		if (bumped) {
			if (moveTo.getOcclusion(rLocSrc) == Occlusion.PARTIALLY_OCCLUDED) {
				return BUMP_PROB_PARTIALLY_OCCLUDED;
			}
			if (moveTo.getOcclusion(rLocSrc) == Occlusion.CLEAR) {
				return BUMP_PROB_CLEAR;
			}
			return BUMP_PROB_BLOCKED;
		}
		return 1 - getConditionalProbabilityHelper(!bumped, rLocSrc, moveTo);
	}
}
