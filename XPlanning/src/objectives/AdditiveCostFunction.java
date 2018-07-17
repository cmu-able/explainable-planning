package objectives;

import java.util.HashMap;
import java.util.Map;

import factors.IAction;
import metrics.IQFunction;
import metrics.IQFunctionDomain;

/**
 * {@link AdditiveCostFunction} represents an additive cost function of n values characterizing n QAs of a single
 * transition. Assume that the single-attribute component cost functions are linear.
 * 
 * @author rsukkerd
 *
 */
public class AdditiveCostFunction implements IAdditiveCostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<AttributeCostFunction<? extends IQFunction<?, ?>>, Double> mScalingConsts = new HashMap<>();
	private String mName;

	// For fast look-up
	private Map<IQFunction<?, ?>, AttributeCostFunction<? extends IQFunction<?, ?>>> mAttrCostFuncs = new HashMap<>();

	public AdditiveCostFunction(String name) {
		mName = name;
	}

	public <E extends IAction, T extends IQFunctionDomain<E>, S extends IQFunction<E, T>> void put(S qFunction,
			AttributeCostFunction<S> attrCostFunc, Double scalingConst) {
		mAttrCostFuncs.put(qFunction, attrCostFunc);
		mScalingConsts.put(attrCostFunc, scalingConst);
	}

	@Override
	public <E extends IAction, T extends IQFunctionDomain<E>, S extends IQFunction<E, T>> AttributeCostFunction<S> getAttributeCostFunction(
			S qFunction) {
		// Casting: We ensure type-safety in put()
		return (AttributeCostFunction<S>) (mAttrCostFuncs.get(qFunction));
	}

	@Override
	public double getScalingConstant(AttributeCostFunction<? extends IQFunction<?, ?>> attrCostFunc) {
		return mScalingConsts.get(attrCostFunc);
	}

	public String getName() {
		return mName;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof AdditiveCostFunction)) {
			return false;
		}
		AdditiveCostFunction costFunc = (AdditiveCostFunction) obj;
		return costFunc.mAttrCostFuncs.equals(mAttrCostFuncs) && costFunc.mScalingConsts.equals(mScalingConsts)
				&& costFunc.mName.equals(mName);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAttrCostFuncs.hashCode();
			result = 31 * result + mScalingConsts.hashCode();
			result = 31 * result + mName.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
