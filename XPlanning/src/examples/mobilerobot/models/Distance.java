package examples.mobilerobot.models;

import language.domain.models.IActionAttribute;

/**
 * {@link Distance} represents a distance attribute of an action. The unit of distance is meter.
 * 
 * @author rsukkerd
 *
 */
public class Distance implements IActionAttribute {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double mDistance;

	public Distance(double distance) {
		mDistance = distance;
	}

	/**
	 * 
	 * @return The distance in meter.
	 */
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
		return Double.compare(distance.mDistance, mDistance) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Double.valueOf(mDistance).hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
