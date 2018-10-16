package examples.clinicscheduling.dsm;

public class ClinicCostProfile {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double mRevenuePerPatient;
	private double mOvertimeCostPerPatient;
	private double mIdleTimeCostPerPatient;
	private double mLeadTimeCostFactor;
	private double mSwitchABPCostFactor;

	public ClinicCostProfile(double revenuePerPatient, double overtimeCostPerPatient, double idleTimeCostPerPatient,
			double leadTimeCostFactor, double switchABPCostFactor) {
		mRevenuePerPatient = revenuePerPatient;
		mOvertimeCostPerPatient = overtimeCostPerPatient;
		mIdleTimeCostPerPatient = idleTimeCostPerPatient;
		mLeadTimeCostFactor = leadTimeCostFactor;
		mSwitchABPCostFactor = switchABPCostFactor;
	}

	public double getRevenuePerPatient() {
		return mRevenuePerPatient;
	}

	public double getOvertimeCostPerPatient() {
		return mOvertimeCostPerPatient;
	}

	public double getIdleTimeCostPerPatient() {
		return mIdleTimeCostPerPatient;
	}

	public double getLeadTimeCostFactor() {
		return mLeadTimeCostFactor;
	}

	public double getSwitchABPCostFactor() {
		return mSwitchABPCostFactor;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ClinicCostProfile)) {
			return false;
		}
		ClinicCostProfile profile = (ClinicCostProfile) obj;
		return Double.compare(profile.mRevenuePerPatient, mRevenuePerPatient) == 0
				&& Double.compare(profile.mOvertimeCostPerPatient, mOvertimeCostPerPatient) == 0
				&& Double.compare(profile.mIdleTimeCostPerPatient, mIdleTimeCostPerPatient) == 0
				&& Double.compare(profile.mLeadTimeCostFactor, mLeadTimeCostFactor) == 0
				&& Double.compare(profile.mSwitchABPCostFactor, mSwitchABPCostFactor) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Double.hashCode(mRevenuePerPatient);
			result = 31 * result + Double.hashCode(mOvertimeCostPerPatient);
			result = 31 * result + Double.hashCode(mIdleTimeCostPerPatient);
			result = 31 * result + Double.hashCode(mLeadTimeCostFactor);
			result = 31 * result + Double.hashCode(mSwitchABPCostFactor);
			hashCode = result;
		}
		return hashCode;
	}
}
