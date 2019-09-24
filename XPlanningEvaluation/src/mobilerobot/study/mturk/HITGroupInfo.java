package mobilerobot.study.mturk;

public class HITGroupInfo {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private String mTitle;
	private String mDescription;
	private String mKeywords;
	private String mReward;
	private Long mAssignmentDuration;
	private Long mLifetime;
	private Integer mMaxAssignments;

	public void setTitle(String title) {
		mTitle = title;
	}

	public void setDescription(String description) {
		mDescription = description;
	}

	public void setKeywords(String keywords) {
		mKeywords = keywords;
	}

	public void setReward(String reward) {
		mReward = reward;
	}

	public void setAssignmentDuration(long assignmentDuration) {
		mAssignmentDuration = assignmentDuration;
	}

	public void setLifetimeInSeconds(long lifetime) {
		mLifetime = lifetime;
	}

	public void setMaxAssignments(int maxAssignments) {
		mMaxAssignments = maxAssignments;
	}

	public String getTitle() {
		return mTitle;
	}

	public String getDescription() {
		return mDescription;
	}

	public String getKeywords() {
		return mKeywords;
	}

	public String getReward() {
		return mReward;
	}

	public Long getAssignmentDuration() {
		return mAssignmentDuration;
	}

	public Long getLifetimeInSeconds() {
		return mLifetime;
	}

	public Integer getMaxAssignments() {
		return mMaxAssignments;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HITGroupInfo)) {
			return false;
		}
		HITGroupInfo hitGroupInfo = (HITGroupInfo) obj;
		return hitGroupInfo.mTitle.equals(mTitle) && hitGroupInfo.mDescription.equals(mDescription)
				&& hitGroupInfo.mKeywords.equals(mKeywords) && hitGroupInfo.mReward.equals(mReward)
				&& hitGroupInfo.mAssignmentDuration.equals(mAssignmentDuration)
				&& hitGroupInfo.mLifetime.equals(mLifetime) && hitGroupInfo.mMaxAssignments.equals(mMaxAssignments);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTitle.hashCode();
			result = 31 * result + mDescription.hashCode();
			result = 31 * result + mKeywords.hashCode();
			result = 31 * result + mReward.hashCode();
			result = 31 * result + mAssignmentDuration.hashCode();
			result = 31 * result + mLifetime.hashCode();
			result = 31 * result + mMaxAssignments.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
