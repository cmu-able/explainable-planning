package examples.mobilerobot.factors;

import exceptions.AttributeNameNotFoundException;
import factors.ICPT;
import factors.StateVar;

/**
 * {@link LocationCPT} is a CPT for a state variable of type {@link Location}.
 * 
 * @author rsukkerd
 *
 */
public class LocationCPT implements ICPT {

	private static final double SUCC_MOVE_PROB = 1.0;
	private static final double FAIL_MOVE_PROB = 1.0;

	public double getConditionalProbability(StateVar<Location> rLocDest, StateVar<Location> rLocSrc,
			MoveToAction moveTo) throws AttributeNameNotFoundException {
		if (rLocDest.getValue().equals(moveTo.getDestination()) && (moveTo.getOcclusion(rLocSrc) == Occlusion.CLEAR
				|| moveTo.getOcclusion(rLocSrc) == Occlusion.PARTIALLY_OCCLUDED)) {
			return SUCC_MOVE_PROB;
		}
		if (rLocDest.getValue().equals(rLocSrc.getValue()) && moveTo.getOcclusion(rLocSrc) == Occlusion.BLOCKED) {
			return FAIL_MOVE_PROB;
		}
		return 0;
	}
}
