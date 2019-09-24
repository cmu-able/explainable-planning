package mobilerobot.study.mturk;

import java.util.Arrays;

public class HITInfo {

	private static final String SANDBOX_PREVIEW_URL_FORMAT = "https://workersandbox.mturk.com/mturk/preview?groupId=%s";
	private static final String PROD_PREVIEW_URL_FORMAT = "https://www.mturk.com/mturk/preview?groupId=%S";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private final String mHITId;
	private final String mHITTypeId;

	private String[] mQuestionDocNames;

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

	public String getHITGroupPreviewURL(boolean sandbox) {
		return String.format(sandbox ? SANDBOX_PREVIEW_URL_FORMAT : PROD_PREVIEW_URL_FORMAT, mHITTypeId);
	}

	public void addQuestionDocumentNames(String[] questionDocNames) {
		mQuestionDocNames = questionDocNames;
	}

	public String[] getQuestionDocumentNames() {
		return mQuestionDocNames;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HITInfo)) {
			return false;
		}
		HITInfo hitInfo = (HITInfo) obj;
		return hitInfo.mHITId.equals(mHITId) && hitInfo.mHITTypeId.equals(mHITTypeId)
				&& Arrays.equals(hitInfo.mQuestionDocNames, mQuestionDocNames);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mHITId.hashCode();
			result = 31 * result + mHITTypeId.hashCode();
			result = 31 * result + Arrays.hashCode(mQuestionDocNames);
			hashCode = result;
		}
		return hashCode;
	}

}
