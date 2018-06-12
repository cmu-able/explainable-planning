package preferences;

import java.util.Iterator;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import metrics.IQFunction;
import metrics.Transition;

/**
 * {@link CostFunction} is a cost function of a regular Markov Decision Process (MDP). This is an additive
 * multi-attribute cost function, whose each single-attribute component cost function is linear.
 * 
 * @author rsukkerd
 *
 */
public class CostFunction implements IAdditiveCostFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private AdditiveCostFunction mCostFunc = new AdditiveCostFunction();

	public CostFunction() {
		// mCostFunc initially empty
	}

	public <E extends IQFunction> void put(E qFunction, AttributeCostFunction<E> attrCostFunc, Double scalingConst) {
		mCostFunc.put(qFunction, attrCostFunc, scalingConst);
	}

	@Override
	public <E extends IQFunction> AttributeCostFunction<E> getAttributeCostFunction(E qFunction) {
		return mCostFunc.getAttributeCostFunction(qFunction);
	}

	@Override
	public double getScalingConstant(AttributeCostFunction<? extends IQFunction> attrCostFunc) {
		return mCostFunc.getScalingConstant(attrCostFunc);
	}

	@Override
	public double getCost(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException {
		return mCostFunc.getCost(transition);
	}

	@Override
	public Iterator<AttributeCostFunction<IQFunction>> iterator() {
		return mCostFunc.iterator();
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
		return costFun.mCostFunc.equals(mCostFunc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mCostFunc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
