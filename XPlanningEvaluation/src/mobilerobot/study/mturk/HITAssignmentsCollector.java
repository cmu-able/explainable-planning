package mobilerobot.study.mturk;

import java.util.List;

import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.Assignment;
import software.amazon.awssdk.services.mturk.model.AssignmentStatus;
import software.amazon.awssdk.services.mturk.model.GetHitRequest;
import software.amazon.awssdk.services.mturk.model.GetHitResponse;
import software.amazon.awssdk.services.mturk.model.HITStatus;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitRequest;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitResponse;
import software.amazon.awssdk.services.mturk.model.RejectAssignmentRequest;

public class HITAssignmentsCollector {

	private static final String REJECT_FEEDBACK = "Rejection feedback";

	private final MTurkClient mClient;

	public HITAssignmentsCollector(MTurkClient client) {
		mClient = client;
	}

	public HITStatus collectHITStatus(HITInfo hitInfo) {
		GetHitRequest getHITRequest = GetHitRequest.builder().hitId(hitInfo.getHITId()).build();
		GetHitResponse getHITResponse = mClient.getHIT(getHITRequest);
		return getHITResponse.hit().hitStatus();
	}

	public List<Assignment> collectPendingReviewHITAssignments(HITInfo hitInfo) {
		// Collect the assignments that have been submitted for this HIT
		// These assignments have not failed the Assignment Review Policy
		List<Assignment> submittedAssignments = collectHITAssignments(hitInfo, AssignmentStatus.SUBMITTED);
		autoRejectSubmittedHITAssignments(submittedAssignments);

		// Collect the remaining assignments that have not been auto-rejected
		// These assignments will be reviewed manually to be approved or rejected
		return collectHITAssignments(hitInfo, AssignmentStatus.SUBMITTED);
	}

	private List<Assignment> collectHITAssignments(HITInfo hitInfo, AssignmentStatus status) {
		// Get the maximum # of completed assignments for this HIT
		ListAssignmentsForHitRequest listHITRequest = ListAssignmentsForHitRequest.builder().hitId(hitInfo.getHITId())
				.assignmentStatuses(status).maxResults(HITPublisher.MAX_ASSIGNMENTS).build();

		ListAssignmentsForHitResponse listHITResponse = mClient.listAssignmentsForHIT(listHITRequest);
		return listHITResponse.assignments();
	}

	private void autoRejectSubmittedHITAssignments(List<Assignment> assignments) {
		for (Assignment assignment : assignments) {
			// TODO: Auto-reject any assignment that doesn't meet minimum criteria
			RejectAssignmentRequest rejectRequest = RejectAssignmentRequest.builder()
					.assignmentId(assignment.assignmentId()).requesterFeedback(REJECT_FEEDBACK).build();

			mClient.rejectAssignment(rejectRequest);
		}
	}

	public List<Assignment> collectApprovedHITAssignments(HITInfo hitInfo) {
		// Collect the assignments that have been manually approved for this HIT
		return collectHITAssignments(hitInfo, AssignmentStatus.APPROVED);
	}
}
