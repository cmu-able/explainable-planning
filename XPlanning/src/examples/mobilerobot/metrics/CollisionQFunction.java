package examples.mobilerobot.metrics;

import exceptions.VarNameNotFoundException;
import mdp.Transition;
import metrics.ICountQFunction;
import metrics.IEvent;

/**
 * {@link CollisionQFunction} determines the collision of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class CollisionQFunction implements ICountQFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private CollisionEvent mCollisionEvent;

	public CollisionQFunction(double speedThreshold) {
		mCollisionEvent = new CollisionEvent(speedThreshold);
	}

	@Override
	public IEvent getEvent() {
		return mCollisionEvent;
	}

	@Override
	public double getValue(Transition trans) throws VarNameNotFoundException {
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
