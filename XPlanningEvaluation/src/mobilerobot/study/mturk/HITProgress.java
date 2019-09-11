package mobilerobot.study.mturk;

import java.util.ArrayList;
import java.util.List;

import software.amazon.awssdk.services.mturk.model.Assignment;

public class HITProgress {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private final HITInfo mHITInfo;
	private final List<Assignment> mCurrentAssignments = new ArrayList<>();

	public HITProgress(HITInfo hitInfo) {
		mHITInfo = hitInfo;
	}

	public void addAssignments(List<Assignment> assignments) {
		mCurrentAssignments.addAll(assignments);
	}

	public List<Assignment> getCurrentAssignments() {
		return mCurrentAssignments;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HITProgress)) {
			return false;
		}
		HITProgress hitProgress = (HITProgress) obj;
		return hitProgress.mHITInfo.equals(mHITInfo) && hitProgress.mCurrentAssignments.equals(mCurrentAssignments);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mHITInfo.hashCode();
			result = 31 * result + mCurrentAssignments.hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
