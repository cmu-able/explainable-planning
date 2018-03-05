package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.VarNameNotFoundException;
import factors.IAction;
import factors.Transition;
import metrics.IEvent;

/**
 * {@link CollisionEvent} represents a collision, where the robot bumps into obstacles at a speed greater than some threshold.
 * 
 * @author rsukkerd
 *
 */
public class CollisionEvent implements IEvent {

	private static final double EPSILON = 0.0001;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double mSpeedThreshold;

	public CollisionEvent(double speedThreshold) {
		mSpeedThreshold = speedThreshold;
	}

	public double getSpeedThreshold() {
		return mSpeedThreshold;
	}

	@Override
	public boolean isEventOccurred(Transition trans) throws VarNameNotFoundException {
		IAction action = trans.getAction();
		RobotBumped rBumpedDest = (RobotBumped) trans.getDestStateVarValue("rBumped");
		RobotSpeed rSpeedSrc = (RobotSpeed) trans.getSrcStateVarValue("rSpeed");
		return action instanceof MoveToAction && rBumpedDest.isBumped() && rSpeedSrc.getSpeed() > mSpeedThreshold;
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
		return Math.abs(event.mSpeedThreshold - mSpeedThreshold) <= EPSILON;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Double.valueOf(mSpeedThreshold).hashCode();
			hashCode = result;
		}
		return result;
	}

}
