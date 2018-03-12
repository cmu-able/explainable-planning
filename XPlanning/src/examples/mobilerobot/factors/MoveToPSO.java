package examples.mobilerobot.factors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import exceptions.AttributeNameNotFoundException;
import factors.IFactoredPSO;
import factors.StateVar;

/**
 * {@link MoveToPSO} is a factored PSO representation of a {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public class MoveToPSO implements IFactoredPSO {

	private static final double SUCC_MOVE_PROB = 1.0;
	private static final double FAIL_MOVE_PROB = 1.0;
	private static final double BUMP_PROB_PARTIALLY_OCCLUDED = 0.2;
	private static final double BUMP_PROB_CLEAR = 0;
	private static final double BUMP_PROB_BLOCKED = 1;

	private MoveToAction mMoveTo;

	public MoveToPSO(MoveToAction moveTo) {
		mMoveTo = moveTo;
	}

	public Map<StateVar<Location>, Double> getLocationEffects(StateVar<Location> rLocSrc)
			throws AttributeNameNotFoundException {
		Map<StateVar<Location>, Double> locationEffects = new HashMap<>();
		Set<Location> possibleLocs = rLocSrc.getPossibleValues();
		for (Location loc : possibleLocs) {
			StateVar<Location> rLocDest = new StateVar<>("rLoc", loc, possibleLocs);
			double prob = getLocationProbability(rLocDest, rLocSrc);
			locationEffects.put(rLocDest, prob);
		}
		return locationEffects;
	}

	public Map<StateVar<RobotBumped>, Double> getRobotBumpedEffects(StateVar<Location> rLocSrc,
			StateVar<RobotBumped> rBumpedSrc) throws AttributeNameNotFoundException {
		Map<StateVar<RobotBumped>, Double> bumpedEffects = new HashMap<>();
		Set<RobotBumped> possibleBumped = rBumpedSrc.getPossibleValues();
		for (RobotBumped bumped : possibleBumped) {
			StateVar<RobotBumped> rBumpedDest = new StateVar<>("rBumped", bumped, possibleBumped);
			double prob = getRobotBumpedProbability(rBumpedDest, rLocSrc);
			bumpedEffects.put(rBumpedDest, prob);
		}
		return bumpedEffects;
	}

	private double getLocationProbability(StateVar<Location> rLocDest, StateVar<Location> rLocSrc)
			throws AttributeNameNotFoundException {
		if ((mMoveTo.getOcclusion(rLocSrc) == Occlusion.CLEAR
				|| mMoveTo.getOcclusion(rLocSrc) == Occlusion.PARTIALLY_OCCLUDED)
				&& rLocDest.getValue().equals(mMoveTo.getDestination())) {
			return SUCC_MOVE_PROB;
		}
		if (mMoveTo.getOcclusion(rLocSrc) == Occlusion.BLOCKED && rLocDest.getValue().equals(rLocSrc.getValue())) {
			return FAIL_MOVE_PROB;
		}
		return 0;
	}

	private double getRobotBumpedProbability(StateVar<RobotBumped> rBumpedDest, StateVar<Location> rLocSrc)
			throws AttributeNameNotFoundException {
		return getRobotBumpedProbabilityHelper(rBumpedDest.getValue().hasBumped(), rLocSrc);
	}

	private double getRobotBumpedProbabilityHelper(boolean bumped, StateVar<Location> rLocSrc)
			throws AttributeNameNotFoundException {
		if (bumped) {
			if (mMoveTo.getOcclusion(rLocSrc) == Occlusion.PARTIALLY_OCCLUDED) {
				return BUMP_PROB_PARTIALLY_OCCLUDED;
			}
			if (mMoveTo.getOcclusion(rLocSrc) == Occlusion.CLEAR) {
				return BUMP_PROB_CLEAR;
			}
			return BUMP_PROB_BLOCKED;
		}
		return 1 - getRobotBumpedProbabilityHelper(!bumped, rLocSrc);
	}
}
