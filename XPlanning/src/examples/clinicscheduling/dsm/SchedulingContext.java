package examples.clinicscheduling.dsm;

public class SchedulingContext {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mCapacity;
	private int mMaxABP;
	private int mMaxQueueSize;
	private ClinicCostProfile mClinicCostProfile;

	public SchedulingContext(int capacity, int maxABP, int maxQueueSize, ClinicCostProfile clinicCostProfile) {
		mCapacity = capacity;
		mMaxABP = maxABP;
		mMaxQueueSize = maxQueueSize;
		mClinicCostProfile = clinicCostProfile;
	}

	public int getCapacity() {
		return mCapacity;
	}

	public int getMaxABP() {
		return mMaxABP;
	}

	public int getMaxQueueSize() {
		return mMaxQueueSize;
	}

	public ClinicCostProfile getClinicCostProfile() {
		return mClinicCostProfile;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SchedulingContext)) {
			return false;
		}
		SchedulingContext context = (SchedulingContext) obj;
		return context.mCapacity == mCapacity && context.mMaxABP == mMaxABP && context.mMaxQueueSize == mMaxQueueSize
				&& context.mClinicCostProfile.equals(mClinicCostProfile);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Integer.hashCode(mCapacity);
			result = 31 * result + Integer.hashCode(mMaxABP);
			result = 31 * result + Integer.hashCode(mMaxQueueSize);
			result = 31 * result + mClinicCostProfile.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
