package preferences;

import java.util.Map;
import java.util.Map.Entry;

import metrics.IQFunction;

/**
 * {@link AdditiveCostFunction} represents an additive cost function of n values characterizing n QAs. If the
 * single-attribute component cost functions are linear, then the input values to this cost function can be either of an
 * entire policy execution, or of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class AdditiveCostFunction implements IMACostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<IQFunction, ? extends ISACostFunction> mSACostFuns;
	private Map<IQFunction, Double> mScalingConsts;

	public AdditiveCostFunction(Map<IQFunction, ? extends ISACostFunction> saCostFuns,
			Map<IQFunction, Double> scalingConsts) {
		mSACostFuns = saCostFuns;
		mScalingConsts = scalingConsts;
	}

	public ISACostFunction getSACostFunction(IQFunction qFunction) {
		return mSACostFuns.get(qFunction);
	}

	public double getScalingConst(IQFunction qFunction) {
		return mScalingConsts.get(qFunction);
	}

	@Override
	public double getCost(Map<IQFunction, Double> values) {
		double result = 0;
		for (Entry<IQFunction, Double> entry : values.entrySet()) {
			IQFunction qFun = entry.getKey();
			double value = entry.getValue();
			result += mScalingConsts.get(qFun) * mSACostFuns.get(qFun).getCost(value);
		}
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof AdditiveCostFunction)) {
			return false;
		}
		AdditiveCostFunction costFun = (AdditiveCostFunction) obj;
		return costFun.mSACostFuns.equals(mSACostFuns) && costFun.mScalingConsts.equals(mScalingConsts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mSACostFuns.hashCode();
			result = 31 * result + mScalingConsts.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
