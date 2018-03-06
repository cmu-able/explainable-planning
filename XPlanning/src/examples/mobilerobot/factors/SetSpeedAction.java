package examples.mobilerobot.factors;

import java.util.List;

import exceptions.AttributeNameNotFoundException;
import factors.Action;
import factors.IAction;
import factors.IActionAttribute;
import factors.IStateVar;

public class SetSpeedAction implements IAction {

	private Action mAction;
	private SpeedStateVar mrSpeedDest;

	public SetSpeedAction(SpeedStateVar rSpeedDest) {
		String actionName = "setSpeed" + rSpeedDest.getSpeed();
		mAction = new Action(actionName, rSpeedDest);
		mrSpeedDest = rSpeedDest;
	}

	public SpeedStateVar getTargetSpeed() {
		return mrSpeedDest;
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

}
