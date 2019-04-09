package language.objectives;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;

/**
 * {@link AdditiveCostFunction} represents an additive cost function of n values characterizing n QAs of a single
 * transition. Assume that the single-attribute component cost functions are linear.
 * 
 * {@link AdditiveCostFunction} may have a positive offset, which is used in formulating SSP to ensure all objective
 * costs are positive, except in the goal states.
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
	private double mOffset;
	private Map<AttributeCostFunction<? extends IQFunction<?, ?>>, Double> mScalingConsts = new HashMap<>();

	// For fast look-up of AttributeCostFunction via IQFunction
	private Map<IQFunction<?, ?>, AttributeCostFunction<? extends IQFunction<?, ?>>> mAttrCostFuncs = new HashMap<>();

	// For caller to obtain a set of generic QA functions
	private Set<IQFunction<IAction, ITransitionStructure<IAction>>> mGenericQFuncs = new HashSet<>();

	// For caller to obtain a set of generic single-attribute cost functions
	private Set<AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>> mGenericAttrCostFuncs = new HashSet<>();

	public AdditiveCostFunction(String name) {
		this(name, 0);
	}

	public AdditiveCostFunction(String name, double offset) {
		mName = name;
		mOffset = offset;
	}

	public <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> void put(
			AttributeCostFunction<S> attrCostFunc, Double scalingConst) {
		mScalingConsts.put(attrCostFunc, scalingConst);

		// For fast look-up
		S qFunction = attrCostFunc.getQFunction();
		mAttrCostFuncs.put(qFunction, attrCostFunc);

		mGenericQFuncs.add((IQFunction<IAction, ITransitionStructure<IAction>>) qFunction);
		mGenericAttrCostFuncs
				.add((AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>) attrCostFunc);
	}

	@Override
	public <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> AttributeCostFunction<S> getAttributeCostFunction(
			S qFunction) {
		// Casting: We ensure type-safety in put()
		return (AttributeCostFunction<S>) (mAttrCostFuncs.get(qFunction));
	}

	@Override
	public double getScalingConstant(AttributeCostFunction<? extends IQFunction<?, ?>> attrCostFunc) {
		return mScalingConsts.get(attrCostFunc);
	}

	@Override
	public String getName() {
		return mName;
	}

	@Override
	public double getOffset() {
		return mOffset;
	}

	public Set<IQFunction<IAction, ITransitionStructure<IAction>>> getQFunctions() {
		return mGenericQFuncs;
	}

	@Override
	public Set<AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>> getAttributeCostFunctions() {
		return mGenericAttrCostFuncs;
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
		return costFunc.mName.equals(mName) && Double.compare(costFunc.mOffset, mOffset) == 0
				&& costFunc.mScalingConsts.equals(mScalingConsts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mName.hashCode();
			result = 31 * result + Double.hashCode(mOffset);
			result = 31 * result + mScalingConsts.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
