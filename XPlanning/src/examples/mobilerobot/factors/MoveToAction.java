package examples.mobilerobot.factors;

import java.util.Arrays;
import java.util.List;

import exceptions.AttributeNameNotFoundException;
import factors.Action;
import factors.IAction;
import factors.IActionAttribute;
import factors.IStateVarValue;

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
	private Location mDest;

	public MoveToAction(Location dest) {
		mDest = dest;
		mAction = new Action("moveToL" + mDest.getId(), dest);
	}

	public void putDistanceValue(Distance distance, IStateVarValue... srcStateVars) {
		mAction.putDerivedAttributeValue("distance", distance, srcStateVars);
	}

	public String getActionName() {
		return mAction.getActionName();
	}

	public Location getDestination() {
		return mDest;
	}

	public Distance getDistance(IStateVarValue... srcStateVars) throws AttributeNameNotFoundException {
		return (Distance) getDerivedAttributeValue("distance", srcStateVars);
	}

	@Override
	public List<IStateVarValue> getParameters() {
		return Arrays.asList(mDest);
	}

	@Override
	public IActionAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		return mAction.getAttributeValue(name);
	}

	@Override
	public IActionAttribute getDerivedAttributeValue(String name, IStateVarValue... srcStateVars) throws AttributeNameNotFoundException {
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
		return moveTo.mAction.equals(mAction) && moveTo.mDest.equals(mDest);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mDest.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
