package examples.mobilerobot.factors;

import factors.IActionAttribute;

/**
 * {@link Distance} represents a distance attribute of an action.
 * 
 * @author rsukkerd
 *
 */
public class Distance implements IActionAttribute {

	private double mDistance;
	
	public Distance(double distance) {
		mDistance = distance;
	}
	
	public double getDistance() {
		return mDistance;
	}
}
