package examples.mobilerobot.factors;

import factors.ICPT;
import factors.StateVar;

/**
 * {@link SpeedCPT} is a CPT for a state variable of type {@link RobotSpeed}.
 * 
 * @author rsukkerd
 *
 */
public class SpeedCPT implements ICPT {

	private static final double SUCC_CONFIG_PROB = 1.0;

	public double getConditionalProbability(StateVar<RobotSpeed> rSpeedDest, StateVar<RobotSpeed> rSpeedSrc,
			SetSpeedAction setSpeed) {
		if (rSpeedDest.getValue().equals(setSpeed.getTargetSpeed())) {
			return SUCC_CONFIG_PROB;
		}
		return 1 - SUCC_CONFIG_PROB;
	}
}
