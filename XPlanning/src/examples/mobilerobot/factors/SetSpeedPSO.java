package examples.mobilerobot.factors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import factors.IFactoredPSO;
import factors.StateVar;
import factors.StateVarDefinition;

/**
 * {@link SetSpeedPSO} is a factored PSO representation of a {@link SetSpeedAction}
 * 
 * @author rsukkerd
 *
 */
public class SetSpeedPSO implements IFactoredPSO {

	private static final double SUCC_SET_SPEED_PROB = 1.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private SetSpeedAction mSetSpeed;

	/**
	 * State variable that is affected by this action.
	 */
	private StateVarDefinition<RobotSpeed> mrSpeedDef;

	public SetSpeedPSO(SetSpeedAction setSpeed, StateVarDefinition<RobotSpeed> rSpeedDef) {
		mSetSpeed = setSpeed;
		mrSpeedDef = rSpeedDef;
	}

	public Map<StateVar<RobotSpeed>, Double> getRobotSpeedEffects() {
		Map<StateVar<RobotSpeed>, Double> speedEffects = new HashMap<>();
		Set<RobotSpeed> possibleSpeeds = mrSpeedDef.getPossibleValues();
		for (RobotSpeed speed : possibleSpeeds) {
			StateVar<RobotSpeed> rSpeedDest = new StateVar<>("rSpeed", speed);
			double prob = getRobotSpeedProbability(rSpeedDest);
			speedEffects.put(rSpeedDest, prob);
		}
		return speedEffects;
	}

	private double getRobotSpeedProbability(StateVar<RobotSpeed> rSpeedDest) {
		if (rSpeedDest.getValue().equals(mSetSpeed.getTargetSpeed())) {
			return SUCC_SET_SPEED_PROB;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SetSpeedPSO)) {
			return false;
		}
		SetSpeedPSO pso = (SetSpeedPSO) obj;
		return pso.mSetSpeed.equals(mSetSpeed) && pso.mrSpeedDef.equals(mrSpeedDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSetSpeed.hashCode();
			result = 31 * result + mrSpeedDef.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
