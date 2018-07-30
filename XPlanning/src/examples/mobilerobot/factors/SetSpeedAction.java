package examples.mobilerobot.factors;

import java.util.List;
import java.util.Set;

import language.exceptions.AttributeNameNotFoundException;
import language.qfactors.Action;
import language.qfactors.IAction;
import language.qfactors.IActionAttribute;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;

/**
 * {@link SetSpeedAction} is a type of actions that set the speed of the robot to a specific value.
 * 
 * @author rsukkerd
 *
 */
public class SetSpeedAction implements IAction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Action mAction;
	private StateVar<RobotSpeed> mrSpeedDest;

	public SetSpeedAction(StateVar<RobotSpeed> rSpeedDest) {
		mAction = new Action("setSpeed", rSpeedDest.getValue());
		mrSpeedDest = rSpeedDest;
	}

	public RobotSpeed getTargetSpeed() {
		return mrSpeedDest.getValue();
	}

	@Override
	public String getName() {
		return mAction.getName();
	}

	@Override
	public String getNamePrefix() {
		return mAction.getNamePrefix();
	}

	@Override
	public List<IStateVarValue> getParameters() {
		return mAction.getParameters();
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

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SetSpeedAction)) {
			return false;
		}
		SetSpeedAction action = (SetSpeedAction) obj;
		return action.mAction.equals(mAction) && action.mrSpeedDest.equals(mrSpeedDest);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAction.hashCode();
			result = 31 * result + mrSpeedDest.hashCode();
			hashCode = result;
		}
		return result;
	}

}
