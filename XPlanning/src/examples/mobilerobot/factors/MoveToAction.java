package examples.mobilerobot.factors;

import java.util.List;

import exceptions.AttributeNameNotFoundException;
import factors.Action;
import factors.IAction;
import factors.IActionAttribute;
import factors.IStateVar;

/**
 * {@link MoveToAction} a type of actions that move the robot to specified destinations. It has an associated distance.
 * 
 * @author rsukkerd
 *
 */
public class MoveToAction implements IAction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Action mAction;
	private LocationStateVar mrLocDest;

	public MoveToAction(LocationStateVar rLocDest) {
		mAction = new Action("moveToL" + mrLocDest.getLocation().getId(), rLocDest);
		mrLocDest = rLocDest;
	}

	public void putDistanceValue(Distance distance, LocationStateVar rLocSrc) {
		mAction.putDerivedAttributeValue("distance", distance, rLocSrc);
	}

	public void putOcclusionValue(Occlusion occlusion, LocationStateVar rLocSrc) {
		mAction.putDerivedAttributeValue("occlusion", occlusion, rLocSrc);
	}

	public LocationStateVar getDestination() {
		return mrLocDest;
	}

	public Distance getDistance(LocationStateVar rLocSrc) throws AttributeNameNotFoundException {
		return (Distance) getDerivedAttributeValue("distance", rLocSrc);
	}

	public Occlusion getOcclusion(LocationStateVar rLocSrc) throws AttributeNameNotFoundException {
		return (Occlusion) getDerivedAttributeValue("occlusion", rLocSrc);
	}

	@Override
	public String getActionName() {
		return mAction.getActionName();
	}

	@Override
	public List<IStateVar> getParameters() {
		return mAction.getParameters();
	}

	@Override
	public IActionAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		return mAction.getAttributeValue(name);
	}

	@Override
	public IActionAttribute getDerivedAttributeValue(String name, IStateVar... srcStateVars)
			throws AttributeNameNotFoundException {
		return mAction.getDerivedAttributeValue(name, srcStateVars);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof MoveToAction)) {
			return false;
		}
		MoveToAction moveTo = (MoveToAction) obj;
		return moveTo.mAction.equals(mAction) && moveTo.mrLocDest.equals(mrLocDest);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mrLocDest.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
