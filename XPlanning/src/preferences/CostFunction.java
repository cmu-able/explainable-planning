package preferences;

import java.util.Map;

import metrics.IQFunction;

/**
 * {@link CostFunction} is a cost function of a regular Markov Decision Process (MDP). This is an additive
 * multi-attribute cost function, whose each single-attribute component cost function is linear.
 * 
 * @author rsukkerd
 *
 */
public class CostFunction implements IMACostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private AdditiveCostFunction mCostFun;

	public CostFunction(Map<IQFunction, ILinearCostFunction> linearCostFuns, Map<IQFunction, Double> scalingConsts) {
		mCostFun = new AdditiveCostFunction(linearCostFuns, scalingConsts);
	}

	public ILinearCostFunction getLinearCostFunction(IQFunction qFunction) {
		return (ILinearCostFunction) mCostFun.getSACostFunction(qFunction);
	}

	public double getScalingConstant(IQFunction qFunction) {
		return mCostFun.getScalingConst(qFunction);
	}

	@Override
	public double getCost(Map<IQFunction, Double> values) {
		return mCostFun.getCost(values);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CostFunction)) {
			return false;
		}
		CostFunction costFun = (CostFunction) obj;
		return costFun.mCostFun.equals(mCostFun);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mCostFun.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
