package examples.mobilerobot.factors;

import factors.IStateVar;
import factors.IStateVarValue;
import factors.StateVar;

/**
 * {@link SpeedStateVar} is a state variable whose value is of type {@link RobotSpeed}.
 * 
 * @author rsukkerd
 *
 */
public class SpeedStateVar implements IStateVar {

	private StateVar mStateVar;
	private RobotSpeed mSpeed;

	public SpeedStateVar(String name, RobotSpeed speed) {
		mStateVar = new StateVar(name, speed);
		mSpeed = speed;
	}

	public RobotSpeed getSpeed() {
		return mSpeed;
	}

	@Override
	public String getName() {
		return mStateVar.getName();
	}

	@Override
	public IStateVarValue getValue() {
		return mStateVar.getValue();
	}

}
