package mobilerobot.study.mturk;

import java.net.URISyntaxException;
import java.util.List;

import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.ApproveAssignmentRequest;
import software.amazon.awssdk.services.mturk.model.Assignment;
import software.amazon.awssdk.services.mturk.model.AssignmentStatus;
import software.amazon.awssdk.services.mturk.model.GetHitRequest;
import software.amazon.awssdk.services.mturk.model.GetHitResponse;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitRequest;
import software.amazon.awssdk.services.mturk.model.ListAssignmentsForHitResponse;

public class ApproveAssignmentsSample {

	/*
	 * Before connecting to MTurk, set up your AWS account and IAM settings as described here:
	 * https://blog.mturk.com/how-to-use-iam-to-control-api-access-to-your-mturk-account-76fe2c2e66e2
	 * 
	 * Configure your AWS credentials as described here:
	 * http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
	 *
	 */

	private final MTurkClient mClient;

	public ApproveAssignmentsSample(final MTurkClient client) {
		mClient = client;
	}

	public void approveAssignment(final String hitId) {
		GetHitRequest getHITRequest = GetHitRequest.builder().hitId(hitId).build();
		GetHitResponse getHITResponse = mClient.getHIT(getHITRequest);
		System.out.println("HIT " + hitId + " status: " + getHITResponse.hit().hitStatus());

		// Get a maximum of 10 completed assignments for this HIT
		ListAssignmentsForHitRequest listHITRequest = ListAssignmentsForHitRequest.builder().hitId(hitId)
				.assignmentStatuses(AssignmentStatus.SUBMITTED).maxResults(10).build();

		ListAssignmentsForHitResponse listHITResponse = mClient.listAssignmentsForHIT(listHITRequest);
		List<Assignment> assignments = listHITResponse.assignments();
		System.out.println("The number of submitted assignments is " + assignments.size());

		// Iterate through all the assignments received
		for (Assignment assignment : assignments) {
			System.out.println("The worker with ID " + assignment.workerId() + " submitted assignment "
					+ assignment.assignmentId() + " and gave the answer " + assignment.answer());

			// Approve the assignment
			ApproveAssignmentRequest approveRequest = ApproveAssignmentRequest.builder()
					.assignmentId(assignment.assignmentId()).requesterFeedback("Good work, thank you!")
					.overrideRejection(false).build();

			mClient.approveAssignment(approveRequest);
			System.out.println("Assignment has been approved: " + assignment.assignmentId());

		}
	}

	// TODO Change this to your HIT ID - see CreateHITSample.java for generating a HIT
	private static final String HIT_ID_TO_APPROVE = "HIT_ID_FROM_HIT_CREATION";

	public static void main(String[] args) throws URISyntaxException {
		final ApproveAssignmentsSample sandboxApp = new ApproveAssignmentsSample(CreateHITSample.getSandboxClient());
		sandboxApp.approveAssignment(HIT_ID_TO_APPROVE);
	}
}
