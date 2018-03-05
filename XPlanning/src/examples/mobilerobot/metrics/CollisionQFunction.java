package examples.mobilerobot.metrics;

import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;
import metrics.CountQFunction;
import metrics.IQFunction;
import metrics.Transition;

/**
 * {@link CollisionQFunction} determines the collision of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class CollisionQFunction implements IQFunction {

	private static final double SPEED_THRESHOLD = 0.4;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private CountQFunction mCountQFn;

	public CollisionQFunction() {
		CollisionEvent collEvent = new CollisionEvent(SPEED_THRESHOLD);
		mCountQFn = new CountQFunction(collEvent);
	}

	@Override
	public double getValue(Transition trans) throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		return mCountQFn.getValue(trans);
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
		return qFun.mCountQFn.equals(mCountQFn);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mCountQFn.hashCode();
			hashCode = result;
		}
		return result;
	}

}
