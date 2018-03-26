package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import metrics.IEvent;
import metrics.Transition;

/**
 * {@link VeryIntrusiveMoveEvent} represents the robot moving into a private area.
 * 
 * @author rsukkerd
 *
 */
public class VeryIntrusiveMoveEvent implements IEvent {

	public boolean isVeryIntrusive(MoveToAction moveTo, StateVar<Location> rLocDest)
			throws AttributeNameNotFoundException {
		return rLocDest.getValue().getArea() == Area.PRIVATE;
	}

	@Override
	public boolean hasEventOccurred(Transition trans) throws VarNotFoundException, AttributeNameNotFoundException {
		if (trans.getAction() instanceof MoveToAction && trans.getDestStateVarValue("rLoc").getValue() instanceof Location) {
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			StateVar<IStateVarValue> locVarDest = trans.getDestStateVarValue("rLoc");
			Location locDest = (Location) locVarDest.getValue();
			StateVar<Location> rLocDest = new StateVar<>("rLoc", locDest);
			return isVeryIntrusive(moveTo, rLocDest);
		}
		return false;
	}

}
