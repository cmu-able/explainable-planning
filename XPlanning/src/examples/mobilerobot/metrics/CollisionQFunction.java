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

	private static double SPEED_THRESHOLD = 0.4;
	
	private CountQFunction mCountQFn;
	
	public CollisionQFunction() {
		CollisionEvent collEvent = new CollisionEvent(SPEED_THRESHOLD);
		mCountQFn = new CountQFunction(collEvent);
	}
	
	@Override
	public double getValue(Transition trans)
			throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		return mCountQFn.getValue(trans);
	}

}
