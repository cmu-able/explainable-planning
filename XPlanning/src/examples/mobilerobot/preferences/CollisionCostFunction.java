package examples.mobilerobot.preferences;

import preferences.ISACostFunction;
import preferences.RiskNeutralCostFunction;

/**
 * {@link CollisionCostFunction} represents a cost function for the number of collisions of the robot. This is a linear
 * cost function.
 * 
 * @author rsukkerd
 *
 */
public class CollisionCostFunction implements ISACostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private RiskNeutralCostFunction mLinearCostFun;

	public CollisionCostFunction(RiskNeutralCostFunction linearCostFun) {
		mLinearCostFun = linearCostFun;
	}

	/**
	 * 
	 * @param collisions
	 *            Either a total (i.e., cumulative) number of collisions of an entire policy execution, or a collision
	 *            of a single transition.
	 * @return The cost representing preference on the value.
	 */
	@Override
	public double getCost(double collisions) {
		return mLinearCostFun.getCost(collisions);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CollisionCostFunction)) {
			return false;
		}
		CollisionCostFunction costFun = (CollisionCostFunction) obj;
		return costFun.mLinearCostFun.equals(mLinearCostFun);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mLinearCostFun.hashCode();
			hashCode = result;
		}
		return result;
	}

}
