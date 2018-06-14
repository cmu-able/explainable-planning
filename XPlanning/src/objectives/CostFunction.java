package objectives;

import java.util.Iterator;

import exceptions.AttributeNameNotFoundException;
import exceptions.VarNotFoundException;
import metrics.IQFunction;
import metrics.Transition;

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

	public <E extends IQFunction> void put(E qFunction, AttributeCostFunction<E> attrCostFunc, Double scalingConst) {
		mAdditiveCostFunc.put(qFunction, attrCostFunc, scalingConst);
	}

	@Override
	public <E extends IQFunction> AttributeCostFunction<E> getAttributeCostFunction(E qFunction) {
		return mAdditiveCostFunc.getAttributeCostFunction(qFunction);
	}

	@Override
	public double getScalingConstant(AttributeCostFunction<? extends IQFunction> attrCostFunc) {
		return mAdditiveCostFunc.getScalingConstant(attrCostFunc);
	}

	@Override
	public double getCost(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException {
		return mAdditiveCostFunc.getCost(transition);
	}

	@Override
	public String getName() {
		return mAdditiveCostFunc.getName();
	}

	@Override
	public Iterator<AttributeCostFunction<IQFunction>> iterator() {
		return mAdditiveCostFunc.iterator();
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
