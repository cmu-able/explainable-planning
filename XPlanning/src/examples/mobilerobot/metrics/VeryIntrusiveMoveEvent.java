package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import exceptions.AttributeNameNotFoundException;
import exceptions.VarNameNotFoundException;
import factors.IAction;
import factors.Transition;
import metrics.IEvent;

/**
 * {@link VeryIntrusiveMoveEvent} represents the robot moving into a private area.
 * 
 * @author rsukkerd
 *
 */
public class VeryIntrusiveMoveEvent implements IEvent {

	@Override
	public boolean hasEventOccurred(Transition trans) throws VarNameNotFoundException, AttributeNameNotFoundException {
		IAction action = trans.getAction();
		Location rLocDest = (Location) trans.getDestStateVar("rLoc");
		return action instanceof MoveToAction && rLocDest.getArea() == Area.PRIVATE;
	}

}
