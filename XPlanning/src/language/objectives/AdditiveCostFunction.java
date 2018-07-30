package language.objectives;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import language.metrics.IQFunction;
import language.metrics.IQFunctionDomain;
import language.qfactors.IAction;

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

	private String mName;
	private Map<AttributeCostFunction<? extends IQFunction<?, ?>>, Double> mScalingConsts = new HashMap<>();

	// For fast look-up of AttributeCostFunction via IQFunction
	private Map<IQFunction<?, ?>, AttributeCostFunction<? extends IQFunction<?, ?>>> mAttrCostFuncs = new HashMap<>();

	// For client to obtain a set of generic QA functions
	private Set<IQFunction<IAction, IQFunctionDomain<IAction>>> mQFunctions = new HashSet<>();

	public AdditiveCostFunction(String name) {
		mName = name;
	}

	public <E extends IAction, T extends IQFunctionDomain<E>, S extends IQFunction<E, T>> void put(S qFunction,
			AttributeCostFunction<S> attrCostFunc, Double scalingConst) {
		mAttrCostFuncs.put(qFunction, attrCostFunc);
		mScalingConsts.put(attrCostFunc, scalingConst);
		mQFunctions.add((IQFunction<IAction, IQFunctionDomain<IAction>>) qFunction);
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

	public Set<IQFunction<IAction, IQFunctionDomain<IAction>>> getQFunctions() {
		return mQFunctions;
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
		return costFunc.mName.equals(mName) && costFunc.mScalingConsts.equals(mScalingConsts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + mScalingConsts.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
