package language.objectives;

import java.util.Set;

import language.domain.models.IAction;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;

/**
 * {@link CostFunction} is a cost function of a regular Markov Decision Process (MDP). This is an additive
 * multi-attribute cost function, whose each single-attribute component cost function is linear, and all scaling
 * constants are between 0 and 1 and sum to 1.
 * 
 * @author rsukkerd
 *
 */
public class CostFunction implements IAdditiveCostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private AdditiveCostFunction mAdditiveCostFunc = new AdditiveCostFunction("cost");

	public CostFunction() {
		// mAdditiveCostFunc initially empty
	}

	public <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> void put(S qFunction,
			AttributeCostFunction<S> attrCostFunc, Double scalingConst) {
		mAdditiveCostFunc.put(qFunction, attrCostFunc, scalingConst);
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

	public Set<IQFunction<IAction, ITransitionStructure<IAction>>> getQFunctions() {
		return mAdditiveCostFunc.getQFunctions();
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
