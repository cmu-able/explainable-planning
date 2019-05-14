package mobilerobot.study.prefinterp;

import java.util.HashMap;
import java.util.Map;

public class WADDPattern {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<String, Double> mWeights = new HashMap<>();

	public void putWeight(String objectiveName, Double weight) {
		mWeights.put(objectiveName, weight);
	}

	public Double getWeight(String objectiveName) {
		return mWeights.get(objectiveName);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof WADDPattern)) {
			return false;
		}
		WADDPattern waddPattern = (WADDPattern) obj;
		return waddPattern.mWeights.equals(mWeights);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mWeights.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
