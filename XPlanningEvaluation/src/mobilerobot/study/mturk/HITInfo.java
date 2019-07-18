package mobilerobot.study.mturk;

public class HITInfo {
	private final String mHITId;
	private final String mHITTypeId;

	public HITInfo(final String hitId, final String hitTypeId) {
		mHITId = hitId;
		mHITTypeId = hitTypeId;
	}

	public String getHITId() {
		return mHITId;
	}

	public String getHITTypeId() {
		return mHITTypeId;
	}

}
