package examples.mobilerobot.metrics;

import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.RobotBumped;
import examples.mobilerobot.models.RobotSpeed;
import language.domain.metrics.IEvent;
import language.domain.metrics.Transition;
import language.exceptions.VarNotFoundException;

/**
 * {@link CollisionEvent} represents a collision, where the robot bumps into obstacles at a speed greater than some
 * threshold.
 * 
 * @author rsukkerd
 *
 */
public class CollisionEvent implements IEvent<MoveToAction, CollisionDomain> {

	public static final String NAME = "collision";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private CollisionDomain mDomain;
	private double mSpeedThreshold;

	public CollisionEvent(CollisionDomain domain, double speedThreshold) {
		mDomain = domain;
		mSpeedThreshold = speedThreshold;
	}

	public double getSpeedThreshold() {
		return mSpeedThreshold;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public CollisionDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public boolean hasEventOccurred(Transition<MoveToAction, CollisionDomain> transition) throws VarNotFoundException {
		RobotSpeed speed = mDomain.getRobotSpeed(transition);
		RobotBumped bumped = mDomain.getRobotBumped(transition);
		return speed.getSpeed() > getSpeedThreshold() && bumped.hasBumped();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CollisionEvent)) {
			return false;
		}
		CollisionEvent event = (CollisionEvent) obj;
		return event.mDomain.equals(mDomain) && Double.compare(event.mSpeedThreshold, mSpeedThreshold) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + Double.valueOf(mSpeedThreshold).hashCode();
			hashCode = result;
		}
		return result;
	}

}
