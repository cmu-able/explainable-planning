package preferences;

import java.util.HashMap;
import java.util.Iterator;
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
public class AdditiveCostFunction implements IAdditiveCostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<AttributeCostFunction<? extends IQFunction>, Double> mScalingConsts = new HashMap<>();
	private String mName;
	// For fast look-up
	private Map<IQFunction, AttributeCostFunction<? extends IQFunction>> mAttrCostFuncs = new HashMap<>();

	public AdditiveCostFunction(String name) {
		mName = name;
	}

	public <E extends IQFunction> void put(E qFunction, AttributeCostFunction<E> attrCostFunc, Double scalingConst) {
		mAttrCostFuncs.put(qFunction, attrCostFunc);
		mScalingConsts.put(attrCostFunc, scalingConst);
	}

	@Override
	public <E extends IQFunction> AttributeCostFunction<E> getAttributeCostFunction(E qFunction) {
		return (AttributeCostFunction<E>) (mAttrCostFuncs.get(qFunction));
	}

	@Override
	public double getScalingConstant(AttributeCostFunction<? extends IQFunction> attrCostFunc) {
		return mScalingConsts.get(attrCostFunc);
	}

	@Override
	public double getCost(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException {
		double cost = 0;
		for (Entry<AttributeCostFunction<? extends IQFunction>, Double> entry : mScalingConsts.entrySet()) {
			AttributeCostFunction<? extends IQFunction> attrCostFunc = entry.getKey();
			double scalingConst = entry.getValue();
			double attrCost = attrCostFunc.getCost(transition);
			double scaledAttrCost = scalingConst * attrCost;
			cost += scaledAttrCost;
		}
		return cost;
	}

	public String getName() {
		return mName;
	}

	@Override
	public Iterator<AttributeCostFunction<IQFunction>> iterator() {
		return new Iterator<AttributeCostFunction<IQFunction>>() {

			private Iterator<AttributeCostFunction<? extends IQFunction>> iter = mScalingConsts.keySet().iterator();

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public AttributeCostFunction<IQFunction> next() {
				return (AttributeCostFunction<IQFunction>) iter.next();
			}
		};
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
