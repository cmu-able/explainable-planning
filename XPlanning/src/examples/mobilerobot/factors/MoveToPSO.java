package examples.mobilerobot.factors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import exceptions.AttributeNameNotFoundException;
import exceptions.EffectClassNotFoundException;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.EffectClass;
import mdp.IActionDescription;
import mdp.IFactoredPSO;

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

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private MoveToAction mMoveTo;

	/**
	 * State variables that are affected by this action.
	 */
	private StateVarDefinition<Location> mrLocDef;
	private StateVarDefinition<RobotBumped> mrBumpedDef;

	/**
	 * Full action descriptions for all independent effect classes of this action
	 */
	private Map<EffectClass, IActionDescription> mActionDescriptions;

	public MoveToPSO(MoveToAction moveTo, StateVarDefinition<Location> rLocDef,
			StateVarDefinition<RobotBumped> rBumpedDef, Set<StateVar<Location>> applicablerLocSrcs)
			throws AttributeNameNotFoundException {
		mMoveTo = moveTo;
		mrLocDef = rLocDef;
		mrBumpedDef = rBumpedDef;
		RobotLocationActionDescription rLocDesc = new RobotLocationActionDescription(moveTo, rLocDef);
		RobotBumpedActionDescription rBumpedDesc = new RobotBumpedActionDescription(mMoveTo, applicablerLocSrcs,
				rBumpedDef);
		mActionDescriptions.put(rLocDesc.getEffectClass(), rLocDesc);
		mActionDescriptions.put(rBumpedDesc.getEffectClass(), rBumpedDesc);
	}

	public Map<StateVar<Location>, Double> getLocationEffects(StateVar<Location> rLocSrc)
			throws AttributeNameNotFoundException {
		Map<StateVar<Location>, Double> locationEffects = new HashMap<>();
		Set<Location> possibleLocs = mrLocDef.getPossibleValues();
		for (Location loc : possibleLocs) {
			StateVar<Location> rLocDest = new StateVar<>("rLoc", loc);
			double prob = getLocationProbability(rLocDest, rLocSrc);
			locationEffects.put(rLocDest, prob);
		}
		return locationEffects;
	}

	public Map<StateVar<RobotBumped>, Double> getRobotBumpedEffects(StateVar<Location> rLocSrc)
			throws AttributeNameNotFoundException {
		Map<StateVar<RobotBumped>, Double> bumpedEffects = new HashMap<>();
		Set<RobotBumped> possibleBumped = mrBumpedDef.getPossibleValues();
		for (RobotBumped bumped : possibleBumped) {
			StateVar<RobotBumped> rBumpedDest = new StateVar<>("rBumped", bumped);
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

	@Override
	public Set<EffectClass> getIndependentEffectClasses() {
		return mActionDescriptions.keySet();
	}

	@Override
	public IActionDescription getActionDescription(EffectClass effectClass) throws EffectClassNotFoundException {
		if (!mActionDescriptions.containsKey(effectClass)) {
			throw new EffectClassNotFoundException(effectClass);
		}
		return mActionDescriptions.get(effectClass);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof MoveToPSO)) {
			return false;
		}
		MoveToPSO pso = (MoveToPSO) obj;
		return pso.mMoveTo.equals(mMoveTo) && pso.mrLocDef.equals(mrLocDef) && pso.mrBumpedDef.equals(mrBumpedDef)
				&& pso.mActionDescriptions.equals(mActionDescriptions);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMoveTo.hashCode();
			result = 31 * result + mrLocDef.hashCode();
			result = 31 * result + mrBumpedDef.hashCode();
			result = 31 * result + mActionDescriptions.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
