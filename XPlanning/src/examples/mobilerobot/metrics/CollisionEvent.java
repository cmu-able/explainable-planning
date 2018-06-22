package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.StateVar;
import factors.StateVarDefinition;
import metrics.IEvent;
import metrics.Transition;
import metrics.TransitionDefinition;

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

	private StateVarDefinition<RobotSpeed> mrSpeedSrcDef;
	private StateVarDefinition<RobotBumped> mrBumpedDestDef;
	private TransitionDefinition mTransitionDef;
	private double mSpeedThreshold;

	public CollisionEvent(StateVarDefinition<RobotSpeed> rSpeedSrcDef, ActionDefinition<MoveToAction> moveToDef,
			StateVarDefinition<RobotBumped> rBumpedDestDef, double speedThreshold) {
		mrSpeedSrcDef = rSpeedSrcDef;
		mrBumpedDestDef = rBumpedDestDef;
		mTransitionDef = new TransitionDefinition(moveToDef);
		mTransitionDef.addSrcStateVarDef(rSpeedSrcDef);
		mTransitionDef.addDestStateVarDef(rBumpedDestDef);
		mSpeedThreshold = speedThreshold;
	}

	public double getSpeedThreshold() {
		return mSpeedThreshold;
	}

	public boolean hasCollided(StateVar<RobotSpeed> rSpeedSrc, MoveToAction moveTo, StateVar<RobotBumped> rBumpedDest) {
		return rBumpedDest.getValue().hasBumped() && rSpeedSrc.getValue().getSpeed() > getSpeedThreshold();
	}

	@Override
	public TransitionDefinition getTransitionDefinition() {
		return mTransitionDef;
	}

	@Override
	public boolean hasEventOccurred(Transition trans) throws VarNotFoundException {
		if (trans.getSrcStateVarValue(mrSpeedSrcDef) instanceof RobotSpeed && trans.getAction() instanceof MoveToAction
				&& trans.getDestStateVarValue(mrBumpedDestDef) instanceof RobotBumped) {
			RobotSpeed speedSrc = (RobotSpeed) trans.getSrcStateVarValue(mrSpeedSrcDef);
			RobotBumped bumpedDest = (RobotBumped) trans.getDestStateVarValue(mrBumpedDestDef);
			StateVar<RobotSpeed> rSpeedSrc = mrSpeedSrcDef.getStateVar(speedSrc);
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			StateVar<RobotBumped> rBumpedDest = mrBumpedDestDef.getStateVar(bumpedDest);
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
		return event.mTransitionDef.equals(mTransitionDef)
				&& Double.compare(event.mSpeedThreshold, mSpeedThreshold) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTransitionDef.hashCode();
			result = 31 * result + Double.valueOf(mSpeedThreshold).hashCode();
			hashCode = result;
		}
		return result;
	}

}
