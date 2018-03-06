package examples.mobilerobot.factors;

import factors.IStateVar;
import factors.IStateVarValue;
import factors.StateVar;

/**
 * {@link BumpedStateVar} is a state variable whose value is of type {@link RobotBumped}.
 * 
 * @author rsukkerd
 *
 */
public class BumpedStateVar implements IStateVar {

	private StateVar mStateVar;
	private RobotBumped mBumped;

	public BumpedStateVar(String name, RobotBumped bumped) {
		mStateVar = new StateVar(name, bumped);
		mBumped = bumped;
	}

	public RobotBumped getBumped() {
		return mBumped;
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
