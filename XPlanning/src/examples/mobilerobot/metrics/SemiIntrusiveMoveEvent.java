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
 * {@link SemiIntrusiveMoveEvent} represents the robot moving into a semi-public area.
 * 
 * @author rsukkerd
 *
 */
public class SemiIntrusiveMoveEvent implements IEvent {

	@Override
	public boolean isEventOccurred(Transition trans) throws VarNameNotFoundException, AttributeNameNotFoundException {
		IAction action = trans.getAction();
		Location rLocDest = (Location) trans.getDestStateVar("rLoc");
		return action instanceof MoveToAction && rLocDest.getArea() == Area.SEMI_PUBLIC;
	}

}
