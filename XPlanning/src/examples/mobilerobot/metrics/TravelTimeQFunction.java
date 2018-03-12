package examples.mobilerobot.metrics;

import java.util.HashSet;
import java.util.Set;

import examples.mobilerobot.factors.Distance;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNameNotFoundException;
import factors.IStateVarValue;
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
		if (trans.getSrcStateVar("rLoc").getValue() instanceof Location
				&& trans.getSrcStateVar("rSpeed").getValue() instanceof RobotSpeed
				&& trans.getAction() instanceof MoveToAction
				&& trans.getDestStateVar("rLoc").getValue() instanceof Location) {
			StateVar<IStateVarValue> locVarSrc = trans.getSrcStateVar("rLoc");
			StateVar<IStateVarValue> speedVarSrc = trans.getSrcStateVar("rSpeed");
			StateVar<IStateVarValue> locVarDest = trans.getDestStateVar("rLoc");
			Location locSrc = (Location) locVarSrc.getValue();
			RobotSpeed speedSrc = (RobotSpeed) speedVarSrc.getValue();
			Location locDest = (Location) locVarDest.getValue();
			Set<Location> possibleLocs = new HashSet<>();
			for (IStateVarValue val : locVarSrc.getPossibleValues()) {
				possibleLocs.add((Location) val);
			}
			Set<RobotSpeed> possibleSpeeds = new HashSet<>();
			for (IStateVarValue val : speedVarSrc.getPossibleValues()) {
				possibleSpeeds.add((RobotSpeed) val);
			}
			StateVar<Location> rLocSrc = new StateVar<>("rLoc", locSrc, possibleLocs);
			StateVar<RobotSpeed> rSpeedSrc = new StateVar<>("rSpeed", speedSrc, possibleSpeeds);
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			StateVar<Location> rLocDest = new StateVar<>("rLoc", locDest, possibleLocs);
			return getTravelTime(rLocSrc, rSpeedSrc, moveTo, rLocDest);
		}
		return 0;
	}

}
