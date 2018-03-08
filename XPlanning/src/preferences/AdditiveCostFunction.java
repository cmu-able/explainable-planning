package preferences;

import java.util.List;

public class AdditiveCostFunction implements IMACostFunction {

	private List<ISACostFunction> mSACostFuns;
	private List<Double> mScalingConsts;

	public AdditiveCostFunction(List<ISACostFunction> saCostFuns, List<Double> scalingConsts) {
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
