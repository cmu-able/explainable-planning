package preferences;

import java.util.List;

/**
 * {@link MDPCostFunction} is a cost function of a regular Markov Decision Process (MDP). This is an additive
 * multi-attribute cost function, whose each single-attribute component cost function is linear.
 * 
 * @author rsukkerd
 *
 */
public class MDPCostFunction implements IMACostFunction {

	private AdditiveCostFunction mCostFun;

	public MDPCostFunction(List<ILinearCostFunction> linearCostFuns, List<Double> scalingConsts) {
		mCostFun = new AdditiveCostFunction(linearCostFuns, scalingConsts);
	}

	@Override
	public double getCost(List<Double> values) {
		return mCostFun.getCost(values);
	}

}
