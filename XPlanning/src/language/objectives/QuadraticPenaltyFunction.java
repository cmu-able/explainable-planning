package language.objectives;

public class QuadraticPenaltyFunction implements IPenaltyFunction {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double mScalingConst;
	private int mNumSamples;

	public QuadraticPenaltyFunction(double scalingConst, int numSamples) {
		mScalingConst = scalingConst;
		mNumSamples = numSamples;
	}

	@Override
	public boolean isNonLinear() {
		return true;
	}

	@Override
	public double getPenalty(double violation) {
		return Math.pow(violation, 2);
	}

	@Override
	public double getScalingConst() {
		return mScalingConst;
	}

	@Override
	public int getNumSamples() {
		return mNumSamples;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof QuadraticPenaltyFunction)) {
			return false;
		}
		QuadraticPenaltyFunction penaltyFunction = (QuadraticPenaltyFunction) obj;
		return Double.compare(penaltyFunction.mScalingConst, mScalingConst) == 0
				&& Integer.compare(penaltyFunction.mNumSamples, mNumSamples) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Double.hashCode(mScalingConst);
			result = 31 * result + Integer.hashCode(mNumSamples);
			hashCode = result;
		}
		return result;
	}
}
