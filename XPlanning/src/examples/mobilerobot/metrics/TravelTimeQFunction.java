package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Distance;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import metrics.IStandardMetricQFunction;
import metrics.Transition;
import metrics.TransitionDefinition;

/**
 * {@link TravelTimeQFunction} calculates the travel time of the robot of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class TravelTimeQFunction implements IStandardMetricQFunction {

	private StateVarDefinition<Location> mrLocSrcDef;
	private StateVarDefinition<RobotSpeed> mrSpeedSrcDef;
	private StateVarDefinition<Location> mrLocDestDef;
	private TransitionDefinition mTransitionDef;

	public TravelTimeQFunction(StateVarDefinition<Location> rLocSrcDef, StateVarDefinition<RobotSpeed> rSpeedSrcDef,
			ActionDefinition<MoveToAction> moveToDef, StateVarDefinition<Location> rLocDestDef) {
		mTransitionDef = new TransitionDefinition(moveToDef);
		mTransitionDef.addSrcStateVarDef(rLocSrcDef);
		mTransitionDef.addSrcStateVarDef(rSpeedSrcDef);
		mTransitionDef.addDestStateVarDef(rLocDestDef);
	}

	public double getTravelTime(StateVar<Location> rLocSrc, StateVar<RobotSpeed> rSpeedSrc, MoveToAction moveTo,
			StateVar<Location> rLocDest) throws AttributeNameNotFoundException {
		Distance distance = moveTo.getDistance(rLocSrc);
		return distance.getDistance() / rSpeedSrc.getValue().getSpeed();
	}

	@Override
	public double getValue(Transition trans)
			throws VarNotFoundException, QValueNotFound, AttributeNameNotFoundException {
		if (trans.getSrcStateVarValue(mrLocSrcDef) instanceof Location
				&& trans.getSrcStateVarValue(mrSpeedSrcDef) instanceof RobotSpeed
				&& trans.getAction() instanceof MoveToAction
				&& trans.getDestStateVarValue(mrLocDestDef) instanceof Location) {
			IStateVarValue locVarSrc = trans.getSrcStateVarValue(mrLocSrcDef);
			IStateVarValue speedVarSrc = trans.getSrcStateVarValue(mrSpeedSrcDef);
			IStateVarValue locVarDest = trans.getDestStateVarValue(mrLocDestDef);
			Location locSrc = (Location) locVarSrc;
			RobotSpeed speedSrc = (RobotSpeed) speedVarSrc;
			Location locDest = (Location) locVarDest;
			StateVar<Location> rLocSrc = new StateVar<>(mrLocSrcDef, locSrc);
			StateVar<RobotSpeed> rSpeedSrc = new StateVar<>(mrSpeedSrcDef, speedSrc);
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			StateVar<Location> rLocDest = new StateVar<>(mrLocDestDef, locDest);
			return getTravelTime(rLocSrc, rSpeedSrc, moveTo, rLocDest);
		}
		return 0;
	}

	@Override
	public TransitionDefinition getTransitionDefinition() {
		return mTransitionDef;
	}

}
