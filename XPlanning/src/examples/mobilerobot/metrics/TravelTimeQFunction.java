package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Distance;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;
import factors.StateVar;
import factors.Transition;
import metrics.IStandardMetricQFunction;

/**
 * {@link TravelTimeQFunction} calculates the travel time of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class TravelTimeQFunction implements IStandardMetricQFunction {

	public double getTravelTime(StateVar<Location> rLocSrc, StateVar<RobotSpeed> rSpeedSrc, MoveToAction moveTo,
			StateVar<Location> rLocDest) throws AttributeNameNotFoundException {
		Distance distance = moveTo.getDistance(rLocSrc);
		return distance.getDistance() / rSpeedSrc.getValue().getSpeed();
	}

	@Override
	public double getValue(Transition trans)
			throws VarNameNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		if (trans.getSrcStateVarValue("rLoc") instanceof Location
				&& trans.getSrcStateVarValue("rSpeed") instanceof RobotSpeed
				&& trans.getAction() instanceof MoveToAction
				&& trans.getDestStateVarValue("rLoc") instanceof Location) {
			StateVar<Location> rLocSrc = new StateVar<>("rLoc", (Location) trans.getSrcStateVarValue("rLoc"));
			StateVar<RobotSpeed> rSpeedSrc = new StateVar<>("rSpeed", (RobotSpeed) trans.getSrcStateVarValue("rSpeed"));
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			StateVar<Location> rLocDest = new StateVar<>("rLoc", (Location) trans.getDestStateVarValue("rLoc"));
			return getTravelTime(rLocSrc, rSpeedSrc, moveTo, rLocDest);
		}
		return 0;
	}

}
