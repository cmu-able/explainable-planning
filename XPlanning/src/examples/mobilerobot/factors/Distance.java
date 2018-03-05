package examples.mobilerobot.factors;

import factors.IActionAttribute;

/**
 * {@link Distance} represents a distance attribute of an action.
 * 
 * @author rsukkerd
 *
 */
public class Distance implements IActionAttribute {

	private static final double EPSILON = 0.0001;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double mDistance;

	public Distance(double distance) {
		mDistance = distance;
	}

	public double getDistance() {
		return mDistance;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Distance)) {
			return false;
		}
		Distance distance = (Distance) obj;
		return Math.abs(distance.mDistance - mDistance) <= EPSILON;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Double.valueOf(mDistance).hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
