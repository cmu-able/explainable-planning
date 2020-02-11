package examples.dart.dsm;

import java.util.Arrays;

public class DartMission {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private TeamConfiguration mIniTeamConfig;
	private int mMaxAltLevel;
	private int mHorizon;
	private double mSensorRange; // at an altitude of r_S or higher, it is not possible to detect targets
	private double mThreatRange; // at an altitude of r_T or higher, threats cannot shoot down the team
	private double mSigma; // factor by which detection probability is reduced due to flying in tight formation
	private double mPsi; // factor by which probability of being destroyed is reduced due to flying in tight
							// formation
	private double[] mExpTargetProbs;
	private double[] mExpThreatProbs;
	private double mTargetWeight;
	private double mThreatWeigth;

	public DartMission(TeamConfiguration iniTeamConfig, int maxAltLevel, int horizon, double sensorRange,
			double threatRange, double sigma, double psi, double[] expTargetProbs, double[] expThreatProbs,
			double targetWeight, double threatWeight) {
		mIniTeamConfig = iniTeamConfig;
		mMaxAltLevel = maxAltLevel;
		mHorizon = horizon;
		mSensorRange = sensorRange;
		mThreatRange = threatRange;
		mSigma = sigma;
		mPsi = psi;
		mExpTargetProbs = expTargetProbs;
		mExpThreatProbs = expThreatProbs;
		mTargetWeight = targetWeight;
		mThreatWeigth = threatWeight;
	}

	public TeamConfiguration getTeamInitialConfiguration() {
		return mIniTeamConfig;
	}

	public int getMaximumAltitudeLevel() {
		return mMaxAltLevel;
	}

	public int getHorizon() {
		return mHorizon;
	}

	public double getSensorRange() {
		return mSensorRange;
	}

	public double getThreatRange() {
		return mThreatRange;
	}

	public double getSigma() {
		return mSigma;
	}

	public double getPsi() {
		return mPsi;
	}

	public double[] getExpectedTargetProbabilities() {
		return mExpTargetProbs;
	}

	public double[] getExpectedThreatProbabilities() {
		return mExpThreatProbs;
	}

	public double getTargetWeight() {
		return mTargetWeight;
	}

	public double getThreatWeight() {
		return mThreatWeigth;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DartMission)) {
			return false;
		}
		DartMission mission = (DartMission) obj;
		return mission.mIniTeamConfig.equals(mIniTeamConfig) && mission.mMaxAltLevel == mMaxAltLevel
				&& mission.mHorizon == mHorizon && Arrays.equals(mission.mExpTargetProbs, mExpTargetProbs)
				&& Double.compare(mission.mSensorRange, mSensorRange) == 0
				&& Double.compare(mission.mThreatRange, mThreatRange) == 0
				&& Double.compare(mission.mSigma, mSigma) == 0 && Double.compare(mission.mPsi, mPsi) == 0
				&& Arrays.equals(mission.mExpThreatProbs, mExpThreatProbs)
				&& Double.compare(mission.mTargetWeight, mTargetWeight) == 0
				&& Double.compare(mission.mThreatWeigth, mThreatWeigth) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mIniTeamConfig.hashCode();
			result = 31 * result + Integer.hashCode(mMaxAltLevel);
			result = 31 * result + Integer.hashCode(mHorizon);
			result = 31 * result + Double.hashCode(mSensorRange);
			result = 31 * result + Double.hashCode(mThreatRange);
			result = 31 * result + Double.hashCode(mSigma);
			result = 31 * result + Double.hashCode(mPsi);
			result = 31 * result + Arrays.hashCode(mExpTargetProbs);
			result = 31 * result + Arrays.hashCode(mExpThreatProbs);
			result = 31 * result + Double.hashCode(mTargetWeight);
			result = 31 * result + Double.hashCode(mThreatWeigth);
			hashCode = result;
		}
		return hashCode;
	}
}
