package language.objectives;

import java.util.Set;

import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;

/**
 * {@link CostFunction} is a cost function of a regular Markov Decision Process (MDP). This is an additive
 * multi-attribute cost function, whose each single-attribute component cost function is linear, and all scaling
 * constants are between 0 and 1 and sum to 1.
 * 
 * {@link CostFunction} may have a positive offset, which is used in formulating SSP to ensure all objective costs are
 * positive, except in the goal states.
 * 
 * @author rsukkerd
 *
 */
public class CostFunction implements IAdditiveCostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private AdditiveCostFunction mAdditiveCostFunc;

	public CostFunction() {
		this(0);
	}

	public CostFunction(double offset) {
		mAdditiveCostFunc = new AdditiveCostFunction("cost", offset);
	}

	public <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> void put(
			AttributeCostFunction<S> attrCostFunc, Double scalingConst) {
		mAdditiveCostFunc.put(attrCostFunc, scalingConst);
	}

	@Override
	public <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> AttributeCostFunction<S> getAttributeCostFunction(
			S qFunction) {
		return mAdditiveCostFunc.getAttributeCostFunction(qFunction);
	}

	@Override
	public double getScalingConstant(AttributeCostFunction<? extends IQFunction<?, ?>> attrCostFunc) {
		return mAdditiveCostFunc.getScalingConstant(attrCostFunc);
	}

	@Override
	public String getName() {
		return mAdditiveCostFunc.getName();
	}

	@Override
	public double getOffset() {
		return mAdditiveCostFunc.getOffset();
	}

	@Override
	public Set<IQFunction<IAction, ITransitionStructure<IAction>>> getQFunctions() {
		return mAdditiveCostFunc.getQFunctions();
	}

	@Override
	public Set<AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>> getAttributeCostFunctions() {
		return mAdditiveCostFunc.getAttributeCostFunctions();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof CostFunction)) {
			return false;
		}
		CostFunction costFunc = (CostFunction) obj;
		return costFunc.mAdditiveCostFunc.equals(mAdditiveCostFunc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAdditiveCostFunc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
