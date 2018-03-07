package examples.mobilerobot.factors;

import java.util.Set;

import exceptions.AttributeNameNotFoundException;
import factors.Action;
import factors.IAction;
import factors.IActionAttribute;
import factors.IStateVarValue;
import factors.StateVar;

public class SetSpeedAction implements IAction {

	private Action mAction;
	private StateVar<RobotSpeed> mrSpeedDest;

	public SetSpeedAction(StateVar<RobotSpeed> rSpeedDest) {
		mAction = new Action("setSpeed" + rSpeedDest.getValue().getSpeed());
		mAction.addParameter(rSpeedDest);
		mrSpeedDest = rSpeedDest;
	}

	public RobotSpeed getTargetSpeed() {
		return mrSpeedDest.getValue();
	}

	@Override
	public IActionAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		return mAction.getAttributeValue(name);
	}

	@Override
	public IActionAttribute getDerivedAttributeValue(String name, Set<StateVar<? extends IStateVarValue>> srcStateVars)
			throws AttributeNameNotFoundException {
		return mAction.getDerivedAttributeValue(name, srcStateVars);
	}

}
