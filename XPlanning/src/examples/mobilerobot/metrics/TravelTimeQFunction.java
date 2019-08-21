package examples.mobilerobot.metrics;

import examples.mobilerobot.models.Distance;
import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.Occlusion;
import examples.mobilerobot.models.RobotSpeed;
import language.domain.metrics.IStandardMetricQFunction;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

/**
 * {@link TravelTimeQFunction} calculates the travel time of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class TravelTimeQFunction implements IStandardMetricQFunction<MoveToAction, TravelTimeDomain> {

	public static final String NAME = "travelTime";

	// For now, assume there is no delay
	private static final double OCCL_DELAY_RATE = 1.0;
	private static final double PARTIAL_OCCL_DELAY_RATE = 1.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private TravelTimeDomain mDomain;

	public TravelTimeQFunction(TravelTimeDomain domain) {
		mDomain = domain;
	}

	public static double getDelayRate(Occlusion occlusion) {
		if (occlusion == Occlusion.OCCLUDED) {
			return OCCL_DELAY_RATE;
		} else if (occlusion == Occlusion.PARTIALLY_OCCLUDED) {
			return PARTIAL_OCCL_DELAY_RATE;
		} else if (occlusion == Occlusion.CLEAR) {
			return 1;
		}
		throw new IllegalArgumentException("Unknown occlusion value: " + occlusion);
	}

	@Override
	public double getValue(Transition<MoveToAction, TravelTimeDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		Distance distance = mDomain.getDistance(transition);
		Occlusion occlusion = mDomain.getOcclusion(transition);
		RobotSpeed speed = mDomain.getRobotSpeed(transition);
		double normalTime = distance.getDistance() / speed.getSpeed();

		if (occlusion == Occlusion.OCCLUDED) {
			return normalTime * OCCL_DELAY_RATE;
		} else if (occlusion == Occlusion.PARTIALLY_OCCLUDED) {
			return normalTime * PARTIAL_OCCL_DELAY_RATE;
		} else if (occlusion == Occlusion.CLEAR) {
			return normalTime;
		}

		throw new IllegalArgumentException("Unknown occlusion value: " + occlusion);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public TravelTimeDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TravelTimeQFunction)) {
			return false;
		}
		TravelTimeQFunction qFunction = (TravelTimeQFunction) obj;
		return qFunction.mDomain.equals(mDomain);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			hashCode = result;
		}
		return result;
	}

}
