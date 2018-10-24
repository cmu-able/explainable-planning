package language.objectives;

import language.domain.metrics.IQFunction;

/**
 * {@link AttributeCostFunction} represents a linear (i.e., risk-neutral) single-attribute cost function of the form
 * C(x) = a + b*x, where b > 0. Since this cost function is linear, its input value can be either a total value
 * characterizing a QA of an entire policy execution, or a value of a single transition.
 * 
 * @author rsukkerd
 *
 * @param <E>
 */
public class AttributeCostFunction<E extends IQFunction<?, ?>> implements ILinearCostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private E mQFunction;
	private double maConst;
	private double mbConst;

	/**
	 * C(x) = a + b*x, where b > 0, and x is a QA value.
	 * 
	 * @param qFunction
	 *            : QA function
	 * @param aConst
	 *            : Constant a
	 * @param bConst
	 *            : Constant b > 0
	 */
	public AttributeCostFunction(E qFunction, double aConst, double bConst) {
		mQFunction = qFunction;
		maConst = aConst;
		mbConst = bConst;
	}

	public E getQFunction() {
		return mQFunction;
	}

	public double getaConst() {
		return maConst;
	}

	public double getbConst() {
		return mbConst;
	}

	@Override
	public String getName() {
		return "cost_" + mQFunction.getName();
	}

	@Override
	public double getCost(double value) {
		return maConst + mbConst * value;
	}

	@Override
	public double inverse(double cost) {
		return (cost - maConst) / mbConst;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof AttributeCostFunction<?>)) {
			return false;
		}
		AttributeCostFunction<?> costFunc = (AttributeCostFunction<?>) obj;
		return costFunc.mQFunction.equals(mQFunction) && Double.compare(costFunc.maConst, maConst) == 0
				&& Double.compare(costFunc.mbConst, mbConst) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mQFunction.hashCode();
			result = 31 * result + Double.hashCode(maConst);
			result = 31 * result + Double.hashCode(mbConst);
			hashCode = result;
		}
		return result;
	}

}
