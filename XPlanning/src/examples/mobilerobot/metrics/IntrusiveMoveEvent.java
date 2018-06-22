package examples.mobilerobot.metrics;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.StateVar;
import factors.StateVarDefinition;
import metrics.IEvent;
import metrics.Transition;
import metrics.TransitionDefinition;

public class IntrusiveMoveEvent implements IEvent {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocDestDef;
	private TransitionDefinition mTransitionDef;
	private Area mArea;

	public IntrusiveMoveEvent(ActionDefinition<MoveToAction> moveTo, StateVarDefinition<Location> rLocDestDef,
			Area area) {
		mrLocDestDef = rLocDestDef;
		mTransitionDef = new TransitionDefinition(moveTo);
		mTransitionDef.addDestStateVarDef(rLocDestDef);
		mArea = area;
	}

	public Area getArea() {
		return mArea;
	}

	public boolean isIntrusive(MoveToAction moveTo, StateVar<Location> rLocDest) throws AttributeNameNotFoundException {
		return rLocDest.getValue().getArea() == getArea();
	}

	@Override
	public TransitionDefinition getTransitionDefinition() {
		return mTransitionDef;
	}

	@Override
	public boolean hasEventOccurred(Transition trans) throws VarNotFoundException, AttributeNameNotFoundException {
		if (trans.getAction() instanceof MoveToAction && trans.getDestStateVarValue(mrLocDestDef) instanceof Location) {
			MoveToAction moveTo = (MoveToAction) trans.getAction();
			Location locDest = (Location) trans.getDestStateVarValue(mrLocDestDef);
			StateVar<Location> rLocDest = mrLocDestDef.getStateVar(locDest);
			return isIntrusive(moveTo, rLocDest);
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IntrusiveMoveEvent)) {
			return false;
		}
		IntrusiveMoveEvent event = (IntrusiveMoveEvent) obj;
		return event.mTransitionDef.equals(mTransitionDef) && event.mArea.equals(mArea);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTransitionDef.hashCode();
			result = 31 * result + mArea.hashCode();
			hashCode = result;
		}
		return result;
	}

}
