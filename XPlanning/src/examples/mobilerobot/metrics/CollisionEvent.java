package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.VarNameNotFoundException;
import factors.StateVar;
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

	public boolean hasCollided(StateVar<RobotSpeed> rSpeedSrc, MoveToAction moveTo, StateVar<RobotBumped> rBumpedDest) {
		return rBumpedDest.getValue().hasBumped() && rSpeedSrc.getValue().getSpeed() > getSpeedThreshold();
	}

	@Override
	public boolean hasEventOccurred(Transition trans) throws VarNameNotFoundException {
		if (trans.getSrcStateVarValue("rSpeed") instanceof RobotSpeed && trans.getAction() instanceof MoveToAction
				&& trans.getDestStateVarValue("rBumped") instanceof RobotBumped) {
			StateVar<RobotSpeed> rSpeedSrc = new StateVar<>("rSpeed", (RobotSpeed) trans.getSrcStateVarValue("rSpeed"));
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			StateVar<RobotBumped> rBumpedDest = new StateVar<>("rBumped",
					(RobotBumped) trans.getDestStateVarValue("rBumped"));
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
