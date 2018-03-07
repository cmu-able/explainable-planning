package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.BumpedStateVar;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.SpeedStateVar;
import exceptions.VarNameNotFoundException;
import factors.Transition;
import metrics.IEvent;

/**
 * {@link CollisionEvent} represents a collision, where the robot bumps into obstacles at a speed greater than some
 * threshold.
 * 
 * @author rsukkerd
 *
 */
public class CollisionEvent implements IEvent {

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

	public boolean hasCollided(SpeedStateVar rSpeedSrc, MoveToAction moveTo, BumpedStateVar rBumpedDest) {
		return rBumpedDest.getBumped().hasBumped() && rSpeedSrc.getSpeed().getSpeed() > getSpeedThreshold();
	}

	@Override
	public boolean hasEventOccurred(Transition trans) throws VarNameNotFoundException {
		if (trans.getSrcStateVar("rSpeed") instanceof SpeedStateVar && trans.getAction() instanceof MoveToAction
				&& trans.getDestStateVar("rBumped") instanceof BumpedStateVar) {
			SpeedStateVar rSpeedSrc = (SpeedStateVar) trans.getSrcStateVar("rSpeed");
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			BumpedStateVar rBumpedDest = (BumpedStateVar) trans.getDestStateVar("rBumped");
			return hasCollided(rSpeedSrc, moveTo, rBumpedDest);
		}
		return false;
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
		return Double.compare(event.mSpeedThreshold, mSpeedThreshold) == 0;
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
