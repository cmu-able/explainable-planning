package examples.mobilerobot.models;

import java.text.DecimalFormat;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarDouble;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link RobotSpeed} is a type of speed value of the robot. The unit of speed is meter/second.
 * 
 * @author rsukkerd
 *
 */
public class RobotSpeed implements IStateVarDouble {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double mSpeed;

	private DecimalFormat decimalFormatter = new DecimalFormat("#.###");

	public RobotSpeed(double speed) {
		mSpeed = speed;
	}

	/**
	 * 
	 * @return The robot's speed in meter/second.
	 */
	public double getSpeed() {
		return mSpeed;
	}

	@Override
	public double getValue() {
		return getSpeed();
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
		return Double.compare(speed.mSpeed, mSpeed) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Double.hashCode(mSpeed);
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return decimalFormatter.format(mSpeed);
	}

}
