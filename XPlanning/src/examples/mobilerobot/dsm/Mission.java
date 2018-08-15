package examples.mobilerobot.dsm;

public class Mission {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mStartNodeID;
	private String mGoalNodeID;
	private String mMapJsonFilename;
	private PreferenceInfo mPrefInfo;

	public Mission(String startNodeID, String goalNodeID, String mapJsonFilename, PreferenceInfo prefInfo) {
		mStartNodeID = startNodeID;
		mGoalNodeID = goalNodeID;
		mMapJsonFilename = mapJsonFilename;
		mPrefInfo = prefInfo;
	}

	public String getStartNodeID() {
		return mStartNodeID;
	}

	public String getGoalNodeID() {
		return mGoalNodeID;
	}

	public String getMapJSONFilename() {
		return mMapJsonFilename;
	}

	public PreferenceInfo getPreferenceInfo() {
		return mPrefInfo;
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
				&& mission.mMapJsonFilename.equals(mMapJsonFilename) && mission.mPrefInfo.equals(mPrefInfo);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mStartNodeID.hashCode();
			result = 31 * result + mGoalNodeID.hashCode();
			result = 31 * result + mMapJsonFilename.hashCode();
			result = 31 * result + mPrefInfo.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
