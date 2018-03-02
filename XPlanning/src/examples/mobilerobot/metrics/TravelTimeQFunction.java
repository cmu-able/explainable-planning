package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Distance;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;
import factors.IAction;
import metrics.IQFunction;
import metrics.Transition;

/**
 * {@link TravelTimeQFunction} calculates the travel time of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class TravelTimeQFunction implements IQFunction {

	@Override
	public double getValue(Transition trans) 
			throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		IAction action = trans.getAction();
		if (action instanceof MoveToAction) {
			Distance distance = (Distance) action.getDerivedAttributeValue("distance", trans.getSrcStateVar("rLoc"));
			RobotSpeed rSpeedSrc = (RobotSpeed) trans.getSrcStateVar("rSpeed");
			return distance.getDistance() / rSpeedSrc.getSpeed();
		} else {
			return 0;
		}
	}

}
