package examples.mobilerobot.dsm;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link PreferenceInfo} contains the scaling constants of the single-attribute cost functions, and the minimum and
 * maximum QA values of any step.
 * 
 * The minimum and maximum QA step-values determine the parameters of the single-attribute cost functions:
 * 
 * C_i(minVal) = a + b * minVal = 0, and
 * 
 * C_i(maxVal) = a + b * maxVal = 1.
 * 
 * Therefore, a = - minVal / (maxVal - minVal), and b = 1 / (maxVal - minVal).
 * 
 * @author rsukkerd
 *
 */
public class PreferenceInfo {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<String, Double> mMinStepQAValues = new HashMap<>();
	private Map<String, Double> mMaxStepQAValues = new HashMap<>();
	private Map<String, Double> mScalingConsts = new HashMap<>();

	public void putMinStepQAValue(String qaName, double minQAValue) {
		mMinStepQAValues.put(qaName, minQAValue);
	}

	public void putMaxStepQAValue(String qaName, double maxQAValue) {
		mMaxStepQAValues.put(qaName, maxQAValue);
	}

	public void putScalingConst(String qaName, double scalingConst) {
		mScalingConsts.put(qaName, scalingConst);
	}

	public double getaConst(String qaName) {
		double minVal = mMinStepQAValues.get(qaName);
		double maxVal = mMaxStepQAValues.get(qaName);
		return -1.0 * minVal / (maxVal - minVal);
	}

	public double getbConst(String qaName) {
		double minVal = mMinStepQAValues.get(qaName);
		double maxVal = mMaxStepQAValues.get(qaName);
		return 1.0 / (maxVal - minVal);
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
		return prefInfo.mMinStepQAValues.equals(mMinStepQAValues) && prefInfo.mMaxStepQAValues.equals(mMaxStepQAValues)
				&& prefInfo.mScalingConsts.equals(mScalingConsts);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mMinStepQAValues.hashCode();
			result = 31 * result + mMaxStepQAValues.hashCode();
			result = 31 * result + mScalingConsts.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
