package examples.mobilerobot.metrics;

import java.util.HashSet;
import java.util.Set;

import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.VarNameNotFoundException;
import factors.IStateVarValue;
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
		if (trans.getSrcStateVar("rSpeed").getValue() instanceof RobotSpeed && trans.getAction() instanceof MoveToAction
				&& trans.getDestStateVar("rBumped").getValue() instanceof RobotBumped) {
			StateVar<IStateVarValue> speedVarSrc = trans.getSrcStateVar("rSpeed");
			StateVar<IStateVarValue> bumpedVarDest = trans.getDestStateVar("rBumped");
			RobotSpeed speedSrc = (RobotSpeed) speedVarSrc.getValue();
			RobotBumped bumpedDest = (RobotBumped) bumpedVarDest.getValue();
			Set<RobotSpeed> possibleSpeeds = new HashSet<>();
			for (IStateVarValue val : speedVarSrc.getPossibleValues()) {
				possibleSpeeds.add((RobotSpeed) val);
			}
			Set<RobotBumped> possibleBumped = new HashSet<>();
			for (IStateVarValue val : bumpedVarDest.getPossibleValues()) {
				possibleBumped.add((RobotBumped) val);
			}
			StateVar<RobotSpeed> rSpeedSrc = new StateVar<>("rSpeed", speedSrc, possibleSpeeds);
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			StateVar<RobotBumped> rBumpedDest = new StateVar<>("rBumped", bumpedDest, possibleBumped);
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
