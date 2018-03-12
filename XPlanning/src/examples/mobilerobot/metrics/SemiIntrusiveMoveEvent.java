package examples.mobilerobot.metrics;

import java.util.HashSet;
import java.util.Set;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import exceptions.AttributeNameNotFoundException;
import exceptions.VarNameNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import factors.Transition;
import metrics.IEvent;

/**
 * {@link SemiIntrusiveMoveEvent} represents the robot moving into a semi-public area.
 * 
 * @author rsukkerd
 *
 */
public class SemiIntrusiveMoveEvent implements IEvent {

	public boolean isSemiIntrusive(MoveToAction moveTo, StateVar<Location> rLocDest)
			throws AttributeNameNotFoundException {
		return rLocDest.getValue().getArea() == Area.SEMI_PUBLIC;
	}

	@Override
	public boolean hasEventOccurred(Transition trans) throws VarNameNotFoundException, AttributeNameNotFoundException {
		if (trans.getAction() instanceof MoveToAction && trans.getDestStateVar("rLoc").getValue() instanceof Location) {
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			StateVar<IStateVarValue> locVarDest = trans.getDestStateVar("rLoc");
			Location locDest = (Location) locVarDest.getValue();
			Set<Location> possibleLocs = new HashSet<>();
			for (IStateVarValue val : locVarDest.getPossibleValues()) {
				possibleLocs.add((Location) val);
			}
			StateVar<Location> rLocDest = new StateVar<>("rLoc", locDest, possibleLocs);
			return isSemiIntrusive(moveTo, rLocDest);
		}
		return false;
	}

}
