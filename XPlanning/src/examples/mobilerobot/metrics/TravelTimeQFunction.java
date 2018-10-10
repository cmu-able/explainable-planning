package examples.mobilerobot.metrics;

import examples.mobilerobot.models.Distance;
import examples.mobilerobot.models.MoveToAction;
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

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private TravelTimeDomain mDomain;

	public TravelTimeQFunction(TravelTimeDomain domain) {
		mDomain = domain;
	}

	@Override
	public double getValue(Transition<MoveToAction, TravelTimeDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		Distance distance = mDomain.getDistance(transition);
		RobotSpeed speed = mDomain.getRobotSpeed(transition);
		return distance.getDistance() / speed.getSpeed();
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
