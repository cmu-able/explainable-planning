package solver.gurobiconnector;

import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class GRBConnectorSettings {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private PrismExplicitModelReader mPrismExplicitModelReader;
	private double mIntFeasTol;
	private double mFeasibilityTol;
	private double mRoundOff;

	public GRBConnectorSettings(PrismExplicitModelReader prismExplicitModelReader) {
		this(prismExplicitModelReader, GRBSolverUtils.DEFAULT_INT_FEAS_TOL, GRBSolverUtils.DEFAULT_FEASIBILITY_TOL,
				GRBSolverUtils.DEFAULT_ROUND_OFF);
	}

	public GRBConnectorSettings(PrismExplicitModelReader prismExplicitModelReader, double intFeasTol,
			double feasibilityTol, double roundOff) {
		mPrismExplicitModelReader = prismExplicitModelReader;
		mIntFeasTol = intFeasTol;
		mFeasibilityTol = feasibilityTol;
		mRoundOff = roundOff;
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

	/**
	 * Any value smaller than the round-off value will be considered zero. This is used for determining if occupation
	 * measure, x_ia of state i and action a, is positive.
	 * 
	 * @return Round-off value
	 */
	public double getRoundOff() {
		return mRoundOff;
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
				&& Double.compare(settings.mIntFeasTol, mIntFeasTol) == 0
				&& Double.compare(settings.mRoundOff, mRoundOff) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mPrismExplicitModelReader.hashCode();
			result = 31 * result + Double.hashCode(mFeasibilityTol);
			result = 31 * result + Double.hashCode(mIntFeasTol);
			result = 31 * result + Double.hashCode(mRoundOff);
			hashCode = result;
		}
		return hashCode;
	}
}
