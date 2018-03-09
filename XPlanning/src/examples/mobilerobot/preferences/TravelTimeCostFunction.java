package examples.mobilerobot.preferences;

import preferences.ILinearCostFunction;
import preferences.RiskNeutralCostFunction;

/**
 * {@link TravelTimeCostFunction} represents a cost function for travel time of the robot. This is a linear cost
 * function. Therefore, an input value to this cost function can be either a total travel time of an entire policy
 * execution, or a travel time of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class TravelTimeCostFunction implements ILinearCostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private RiskNeutralCostFunction mLinearCostFun;

	public TravelTimeCostFunction(RiskNeutralCostFunction linearCostFun) {
		mLinearCostFun = linearCostFun;
	}

	/**
	 * 
	 * @param travelTime
	 *            Either a total (i.e., cumulative) travel time of an entire policy execution, or a travel time of a
	 *            single transition.
	 * @return The cost representing preference on the value.
	 */
	@Override
	public double getCost(double travelTime) {
		return mLinearCostFun.getCost(travelTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TravelTimeCostFunction)) {
			return false;
		}
		TravelTimeCostFunction costFun = (TravelTimeCostFunction) obj;
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
