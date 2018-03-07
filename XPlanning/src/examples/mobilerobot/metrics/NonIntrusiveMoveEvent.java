package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import exceptions.AttributeNameNotFoundException;
import exceptions.VarNameNotFoundException;
import factors.StateVar;
import factors.Transition;
import metrics.IEvent;

/**
 * {@link NonIntrusiveMoveEvent} represents the robot moving into a public area.
 * 
 * @author rsukkerd
 *
 */
public class NonIntrusiveMoveEvent implements IEvent {

	public boolean isNonIntrusive(MoveToAction moveTo, StateVar<Location> rLocDest)
			throws AttributeNameNotFoundException {
		return rLocDest.getValue().getArea() == Area.PUBLIC;
	}

	@Override
	public boolean hasEventOccurred(Transition trans) throws VarNameNotFoundException, AttributeNameNotFoundException {
		if (trans.getAction() instanceof MoveToAction && trans.getDestStateVarValue("rLoc") instanceof Location) {
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			StateVar<Location> rLocDest = new StateVar<>("rLoc", (Location) trans.getDestStateVarValue("rLoc"));
			return isNonIntrusive(moveTo, rLocDest);
		}
		return false;
	}

}
