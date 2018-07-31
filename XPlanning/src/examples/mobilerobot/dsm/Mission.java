package examples.mobilerobot.dsm;

public class Mission {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mStartNodeID;
	private String mGoalNodeID;
	private double mMaxTravelTime;

	public Mission(String startNodeID, String goalNodeID, double maxTravelTime) {
		mStartNodeID = startNodeID;
		mGoalNodeID = goalNodeID;
		mMaxTravelTime = maxTravelTime;
	}

	public String getStartNodeID() {
		return mStartNodeID;
	}

	public String getGoalNodeID() {
		return mGoalNodeID;
	}

	public double getMaxTravelTime() {
		return mMaxTravelTime;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Mission)) {
			return false;
		}
		Mission mission = (Mission) obj;
		return mission.mStartNodeID.equals(mStartNodeID) && mission.mGoalNodeID.equals(mGoalNodeID)
				&& Double.compare(mission.mMaxTravelTime, mMaxTravelTime) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStartNodeID.hashCode();
			result = 31 * result + mGoalNodeID.hashCode();
			result = 31 * result + Double.hashCode(mMaxTravelTime);
			hashCode = result;
		}
		return hashCode;
	}
}
