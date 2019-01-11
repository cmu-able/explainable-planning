package explanation.analysis;

import java.util.Map;

import language.domain.metrics.IQFunction;

public class WeberScale {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<IQFunction<?, ?>, Double> mPercentIncreaseMapping;

	public WeberScale(Map<IQFunction<?, ?>, Double> percentIncreaseMapping) {
		mPercentIncreaseMapping = percentIncreaseMapping;
	}

	public double getSignificantIncrease(IQFunction<?, ?> qFunction, double currentQAValue) {
		double percentIncrease = mPercentIncreaseMapping.get(qFunction);
		return currentQAValue * (1.0 + percentIncrease);
	}

	public double getSignificantDecrease(IQFunction<?, ?> qFunction, double currentQAValue) {
		double percentIncrease = mPercentIncreaseMapping.get(qFunction);
		return currentQAValue / (1.0 + percentIncrease);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof WeberScale)) {
			return false;
		}
		WeberScale weberScale = (WeberScale) obj;
		return weberScale.mPercentIncreaseMapping.equals(mPercentIncreaseMapping);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPercentIncreaseMapping.hashCode();
			hashCode = result;
		}
		return result;
	}
}
