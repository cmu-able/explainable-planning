package explanation.analysis;

import java.util.HashMap;
import java.util.Map;

import language.domain.metrics.IQFunction;
import language.objectives.CostFunction;

public class DifferenceScaler {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<IQFunction<?, ?>, Double> mPercentImprovement = new HashMap<>();
	private CostFunction mCostFunction;

	public DifferenceScaler(CostFunction costFunction) {
		mCostFunction = costFunction;
	}

	public void putPercentImprovement(IQFunction<?, ?> qFunction, double percentImprovement) {
		mPercentImprovement.put(qFunction, percentImprovement);
	}

	public double getSignificantImprovement(IQFunction<?, ?> qFunction, double currentQAValue) {
		double percentImprove = mPercentImprovement.get(qFunction);
		double attrCostFuncSlope = mCostFunction.getAttributeCostFunction(qFunction).getSlope();
		return attrCostFuncSlope > 0 ? currentQAValue - (percentImprove * currentQAValue)
				: currentQAValue + (percentImprove * currentQAValue);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DifferenceScaler)) {
			return false;
		}
		DifferenceScaler weberScale = (DifferenceScaler) obj;
		return weberScale.mPercentImprovement.equals(mPercentImprovement);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPercentImprovement.hashCode();
			result = 31 * result + mCostFunction.hashCode();
			hashCode = result;
		}
		return result;
	}
}
