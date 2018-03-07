package examples.mobilerobot.factors;

import exceptions.AttributeNameNotFoundException;
import factors.IStateVarAttribute;
import factors.IStateVarValue;

/**
 * {@link RobotBumped} is a type of bump sensor value (boolean) of the robot.
 * 
 * @author rsukkerd
 *
 */
public class RobotBumped implements IStateVarValue {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private boolean mBumped;

	public RobotBumped(boolean bumped) {
		mBumped = bumped;
	}

	public boolean hasBumped() {
		return mBumped;
	}

	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		throw new AttributeNameNotFoundException(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RobotBumped)) {
			return false;
		}
		RobotBumped bumped = (RobotBumped) obj;
		return bumped.mBumped == mBumped;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Boolean.valueOf(mBumped).hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
