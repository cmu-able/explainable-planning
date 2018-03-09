package examples.mobilerobot.preferences;

import preferences.ILinearCostFunction;
import preferences.RiskNeutralCostFunction;

/**
 * {@link IntrusivenessCostFunction} represents a cost function for intrusiveness of the robot. This is a linear cost
 * function.
 * 
 * @author rsukkerd
 *
 */
public class IntrusivenessCostFunction implements ILinearCostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private RiskNeutralCostFunction mLinearCostFun;

	public IntrusivenessCostFunction(RiskNeutralCostFunction linearCostFun) {
		mLinearCostFun = linearCostFun;
	}

	/**
	 * 
	 * @param intrusiveness
	 *            Either a total (i.e., cumulative) intrusiveness of an entire policy execution, or an intrusiveness of
	 *            a single transition.
	 * @return The cost representing preference on the value.
	 */
	@Override
	public double getCost(double intrusiveness) {
		return mLinearCostFun.getCost(intrusiveness);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof IntrusivenessCostFunction)) {
			return false;
		}
		IntrusivenessCostFunction costFun = (IntrusivenessCostFunction) obj;
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
