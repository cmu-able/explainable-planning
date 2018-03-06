package examples.mobilerobot.factors;

import factors.ICPT;

/**
 * {@link SpeedCPT} is a CPT for {@link SpeedStateVar}.
 * 
 * @author rsukkerd
 *
 */
public class SpeedCPT implements ICPT {

	private static final double SUCC_CONFIG_PROB = 1.0;

	public double getConditionalProbability(SpeedStateVar rSpeedDest, SpeedStateVar rSpeedSrc,
			SetSpeedAction setSpeed) {
		if (rSpeedDest.getSpeed().equals(setSpeed.getTargetSpeed().getSpeed())) {
			return SUCC_CONFIG_PROB;
		}
		return 1 - SUCC_CONFIG_PROB;
	}
}
