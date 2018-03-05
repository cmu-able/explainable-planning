package examples.mobilerobot.factors;

import exceptions.AttributeNameNotFoundException;
import factors.IStateVarAttribute;
import factors.IStateVarValue;

/**
 * {@link RobotSpeed} is a type of speed value of the robot.
 * 
 * @author rsukkerd
 *
 */
public class RobotSpeed implements IStateVarValue {

	private static final double EPSILON = 0.0001;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double mSpeed;

	public RobotSpeed(double speed) {
		mSpeed = speed;
	}

	public double getSpeed() {
		return mSpeed;
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
		if (!(obj instanceof RobotSpeed)) {
			return false;
		}
		RobotSpeed speed = (RobotSpeed) obj;
		return Math.abs(speed.mSpeed - mSpeed) <= EPSILON;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Double.valueOf(mSpeed).hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
