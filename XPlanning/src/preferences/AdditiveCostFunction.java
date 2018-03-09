package preferences;

import java.util.List;

/**
 * {@link AdditiveCostFunction} represents an additive cost function of n values characterizing n QAs. If the
 * single-attribute component cost functions are linear, then the input values to this cost function can be either of an
 * entire policy execution, or of a single transition.
 * 
 * @author rsukkerd
 *
 */
public class AdditiveCostFunction implements IMACostFunction {

	private List<? extends ISACostFunction> mSACostFuns;
	private List<Double> mScalingConsts;

	public AdditiveCostFunction(List<? extends ISACostFunction> saCostFuns, List<Double> scalingConsts) {
		mSACostFuns = saCostFuns;
		mScalingConsts = scalingConsts;
	}

	@Override
	public double getCost(List<Double> values) {
		double result = 0;
		for (int i = 0; i < values.size(); i++) {
			double value = values.get(i);
			result += mScalingConsts.get(i) * mSACostFuns.get(i).getCost(value);
		}
		return result;
	}

}
