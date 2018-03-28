package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Distance;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotSpeed;
import exceptions.AttributeNameNotFoundException;
import exceptions.QValueNotFound;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
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

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocSrcDef;
	private StateVarDefinition<RobotSpeed> mrSpeedSrcDef;
	private StateVarDefinition<Location> mrLocDestDef;
	private TransitionDefinition mTransitionDef;

	public TravelTimeQFunction(StateVarDefinition<Location> rLocSrcDef, StateVarDefinition<RobotSpeed> rSpeedSrcDef,
			ActionDefinition<MoveToAction> moveToDef, StateVarDefinition<Location> rLocDestDef) {
		mrLocSrcDef = rLocSrcDef;
		mrSpeedSrcDef = rSpeedSrcDef;
		mrLocDestDef = rLocDestDef;
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
			Location locSrc = (Location) trans.getSrcStateVarValue(mrLocSrcDef);
			RobotSpeed speedSrc = (RobotSpeed) trans.getSrcStateVarValue(mrSpeedSrcDef);
			Location locDest = (Location) trans.getDestStateVarValue(mrLocDestDef);
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TravelTimeQFunction)) {
			return false;
		}
		TravelTimeQFunction qFunc = (TravelTimeQFunction) obj;
		return qFunc.mTransitionDef.equals(mTransitionDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTransitionDef.hashCode();
			hashCode = result;
		}
		return result;
	}

}
