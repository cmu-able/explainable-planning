package preferences;

/**
 * {@link RiskNeutralCostFunction} represents a linear (i.e., risk-neutral) single-attribute cost function. Since this
 * cost function is linear, its input value can be either a total value characterizing a QA of an entire policy
 * execution, or a value of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class RiskNeutralCostFunction implements ILinearCostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double maConst;
	private double mbConst;

	public RiskNeutralCostFunction(double aConst, double bConst) {
		maConst = aConst;
		mbConst = bConst;
	}

	/**
	 * 
	 * @param value
	 *            Either a total (i.e., cumulative) value characterizing a QA of an entire policy execution, or a value
	 *            of a single transition.
	 * @return The cost representing preference on the value.
	 */
	@Override
	public double getCost(double value) {
		return maConst + mbConst * value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RiskNeutralCostFunction)) {
			return false;
		}
		RiskNeutralCostFunction costFun = (RiskNeutralCostFunction) obj;
		return Double.compare(costFun.maConst, maConst) == 0 && Double.compare(costFun.mbConst, mbConst) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Double.valueOf(maConst).hashCode();
			result = 31 * result + Double.valueOf(mbConst).hashCode();
			hashCode = result;
		}
		return result;
	}

}
