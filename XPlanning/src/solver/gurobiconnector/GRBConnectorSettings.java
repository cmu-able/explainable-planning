package solver.gurobiconnector;

import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class GRBConnectorSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PrismExplicitModelReader mPrismExplicitModelReader;
	private double mFeasibilityTol;
	private double mIntFeasTol;

	public GRBConnectorSettings(PrismExplicitModelReader prismExplicitModelReader, double feasibilityTol,
			double intFeasTol) {
		mPrismExplicitModelReader = prismExplicitModelReader;
		mFeasibilityTol = feasibilityTol;
		mIntFeasTol = intFeasTol;
	}

	public PrismExplicitModelReader getPrismExplicitModelReader() {
		return mPrismExplicitModelReader;
	}

	public double getFeasibilityTolerance() {
		return mFeasibilityTol;
	}

	public double getIntegralityTolerance() {
		return mIntFeasTol;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof GRBConnectorSettings)) {
			return false;
		}
		GRBConnectorSettings settings = (GRBConnectorSettings) obj;
		return settings.mPrismExplicitModelReader.equals(mPrismExplicitModelReader)
				&& Double.compare(settings.mFeasibilityTol, mFeasibilityTol) == 0
				&& Double.compare(settings.mIntFeasTol, mIntFeasTol) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPrismExplicitModelReader.hashCode();
			result = 31 * result + Double.hashCode(mFeasibilityTol);
			result = 31 * result + Double.hashCode(mIntFeasTol);
			hashCode = result;
		}
		return hashCode;
	}
}
