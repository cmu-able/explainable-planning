package preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import metrics.IQFunction;
import metrics.Transition;

/**
 * {@link AdditiveCostFunction} represents an additive cost function of n values characterizing n QAs of a single
 * transition. Assume that the single-attribute component cost functions are linear.
 * 
 * @author rsukkerd
 *
 */
public class AdditiveCostFunction implements IMACostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<IQFunction, AttributeCostFunction<? extends IQFunction>> mAttrCostFuncs = new HashMap<>();
	private Map<IQFunction, Double> mScalingConsts = new HashMap<>();

	public AdditiveCostFunction() {
		// mAttrCostFuncs and mScalingConsts initially empty
	}

	public <E extends IQFunction> void put(E qFunction, AttributeCostFunction<E> attrCostFunc, Double scalingConst) {
		mAttrCostFuncs.put(qFunction, attrCostFunc);
		mScalingConsts.put(qFunction, scalingConst);
	}

	@Override
	public <E extends IQFunction> AttributeCostFunction<E> getAttributeCostFunction(E qFunction) {
		return (AttributeCostFunction<E>) (mAttrCostFuncs.get(qFunction));
	}

	@Override
	public double getScalingConstant(IQFunction qFunction) {
		return mScalingConsts.get(qFunction);
	}

	@Override
	public double getCost(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException {
		double cost = 0;
		for (Entry<IQFunction, AttributeCostFunction<? extends IQFunction>> entry : mAttrCostFuncs.entrySet()) {
			IQFunction qFunc = entry.getKey();
			AttributeCostFunction<? extends IQFunction> attrCostFunc = entry.getValue();
			double attrCost = attrCostFunc.getCost(transition);
			double scaledAttrCost = mScalingConsts.get(qFunc) * attrCost;
			cost += scaledAttrCost;
		}
		return cost;
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
		return costFun.mAttrCostFuncs.equals(mAttrCostFuncs) && costFun.mScalingConsts.equals(mScalingConsts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAttrCostFuncs.hashCode();
			result = 31 * result + mScalingConsts.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
