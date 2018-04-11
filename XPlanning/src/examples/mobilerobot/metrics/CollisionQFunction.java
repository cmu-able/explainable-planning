package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.StateVarDefinition;
import metrics.ICountQFunction;
import metrics.IEvent;
import metrics.Transition;
import metrics.TransitionDefinition;

/**
 * {@link CollisionQFunction} determines the collision of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class CollisionQFunction implements ICountQFunction {

	private static final String NAME = "collision";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private CollisionEvent mCollisionEvent;

	public CollisionQFunction(StateVarDefinition<RobotSpeed> rSpeedSrcDef, ActionDefinition<MoveToAction> moveToDef,
			StateVarDefinition<RobotBumped> rBumpedDestDef, double speedThreshold) {
		mCollisionEvent = new CollisionEvent(rSpeedSrcDef, moveToDef, rBumpedDestDef, speedThreshold);
	}

	@Override
	public IEvent getEvent() {
		return mCollisionEvent;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public TransitionDefinition getTransitionDefinition() {
		return mCollisionEvent.getTransitionDefinition();
	}

	@Override
	public double getValue(Transition trans) throws VarNotFoundException {
		return mCollisionEvent.hasEventOccurred(trans) ? 1 : 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CollisionQFunction)) {
			return false;
		}
		CollisionQFunction qFun = (CollisionQFunction) obj;
		return qFun.mCollisionEvent.equals(mCollisionEvent);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mCollisionEvent.hashCode();
			hashCode = result;
		}
		return result;
	}

}
