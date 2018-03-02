package examples.mobilerobot.factors;

import exceptions.AttributeNameNotFoundException;
import factors.IStateVarAttribute;
import factors.IStateVarValue;

public class RobotBumped implements IStateVarValue {

	private boolean mBumped;
	
	public RobotBumped(boolean bumped) {
		mBumped = bumped;
	}
	
	public boolean isBumped() {
		return mBumped;
	}
	
	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		throw new AttributeNameNotFoundException(name);
	}

}
