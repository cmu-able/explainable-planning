package examples.mobilerobot.factors;

import java.util.Arrays;
import java.util.List;

import exceptions.AttributeNameNotFoundException;
import factors.Action;
import factors.IAction;
import factors.IActionAttribute;
import factors.IStateVarValue;

/**
 * {@link MoveToAction} a type of actions that move the robot to specified destinations.
 * It has an associated distance.
 * 
 * @author rsukkerd
 *
 */
public class MoveToAction implements IAction {

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
	public IActionAttribute getDerivedAttributeValue(String name, IStateVarValue... srcStateVars) 
			throws AttributeNameNotFoundException {
		return mAction.getDerivedAttributeValue(name, srcStateVars);
	}
}
