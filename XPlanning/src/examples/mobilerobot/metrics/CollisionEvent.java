package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.VarNameNotFoundException;
import factors.IAction;
import metrics.IEvent;
import metrics.Transition;

/**
 * {@link CollisionEvent} represents a collision, where the robot bumps into obstacles at a speed greater than some threshold.
 * 
 * @author rsukkerd
 *
 */
public class CollisionEvent implements IEvent {

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
		RobotBumped rBumpedDest = (RobotBumped) trans.getDestStateVar("rBumped");
		RobotSpeed rSpeedSrc = (RobotSpeed) trans.getSrcStateVar("rSpeed");
		return action instanceof MoveToAction && rBumpedDest.isBumped() && rSpeedSrc.getSpeed() > mSpeedThreshold;
	}

}
