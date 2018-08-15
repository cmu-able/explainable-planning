package examples.mobilerobot.dsm;

import java.util.HashMap;
import java.util.Map;

public class PreferenceInfo {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<String, Double> mMinQAValues = new HashMap<>();
	private Map<String, Double> mMaxQAValues = new HashMap<>();
	private Map<String, Double> mScalingConsts = new HashMap<>();

	public void putMinQAValue(String qaName, double minQAValue) {
		mMinQAValues.put(qaName, minQAValue);
	}

	public void putMaxQAValue(String qaName, double maxQAValue) {
		mMaxQAValues.put(qaName, maxQAValue);
	}

	public void putScalingConst(String qaName, double scalingConst) {
		mScalingConsts.put(qaName, scalingConst);
	}

	public double getMinQAValue(String qaName) {
		return mMinQAValues.get(qaName);
	}

	public double getMaxQAValue(String qaName) {
		return mMaxQAValues.get(qaName);
	}

	public double getScalingConst(String qaName) {
		return mScalingConsts.get(qaName);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof PreferenceInfo)) {
			return false;
		}
		PreferenceInfo prefInfo = (PreferenceInfo) obj;
		return prefInfo.mMinQAValues.equals(mMinQAValues) && prefInfo.mMaxQAValues.equals(mMaxQAValues)
				&& prefInfo.mScalingConsts.equals(mScalingConsts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMinQAValues.hashCode();
			result = 31 * result + mMaxQAValues.hashCode();
			result = 31 * result + mScalingConsts.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
