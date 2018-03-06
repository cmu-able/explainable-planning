package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Distance;
import examples.mobilerobot.factors.LocationStateVar;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotSpeed;
import examples.mobilerobot.factors.SpeedStateVar;
import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;
import factors.Transition;
import metrics.IQFunction;

/**
 * {@link TravelTimeQFunction} calculates the travel time of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class TravelTimeQFunction implements IQFunction {

	public double getTravelTime(LocationStateVar rLocSrc, SpeedStateVar rSpeedSrc, MoveToAction moveTo,
			LocationStateVar rLocDest) throws AttributeNameNotFoundException {
		Distance distance = moveTo.getDistance(rLocSrc);
		RobotSpeed speed = rSpeedSrc.getSpeed();
		return distance.getDistance() / speed.getSpeed();
	}

	@Override
	public double getValue(Transition trans)
			throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		if (trans.getSrcStateVar("rLoc") instanceof LocationStateVar
				&& trans.getSrcStateVar("rSpeed") instanceof SpeedStateVar && trans.getAction() instanceof MoveToAction
				&& trans.getDestStateVar("rLoc") instanceof LocationStateVar) {
			LocationStateVar rLocSrc = (LocationStateVar) trans.getSrcStateVar("rLoc");
			SpeedStateVar rSpeedSrc = (SpeedStateVar) trans.getSrcStateVar("rSpeed");
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			LocationStateVar rLocDest = (LocationStateVar) trans.getSrcStateVar("rLoc");
			return getTravelTime(rLocSrc, rSpeedSrc, moveTo, rLocDest);
		}
		return 0;
	}

}
