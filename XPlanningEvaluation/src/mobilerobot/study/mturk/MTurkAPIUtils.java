package mobilerobot.study.mturk;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.MTurkClientBuilder;
import software.amazon.awssdk.services.mturk.model.Assignment;
import software.amazon.awssdk.services.mturk.model.AssignmentStatus;
import software.amazon.awssdk.services.mturk.model.DeleteHitRequest;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceRequest;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceResponse;
import software.amazon.awssdk.services.mturk.model.HIT;
import software.amazon.awssdk.services.mturk.model.HITStatus;
import software.amazon.awssdk.services.mturk.model.ListHiTsRequest;
import software.amazon.awssdk.services.mturk.model.ListHiTsResponse;
import software.amazon.awssdk.services.mturk.model.ParameterMapEntry;
import software.amazon.awssdk.services.mturk.model.PolicyParameter;
import software.amazon.awssdk.services.mturk.model.ReviewPolicy;
import software.amazon.awssdk.services.mturk.model.UpdateExpirationForHitRequest;

public class MTurkAPIUtils {

	private static final String SANDBOX_ENDPOINT = "https://mturk-requester-sandbox.us-east-1.amazonaws.com";
	private static final String PROD_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com";

	private static final String AUTO_REJECT_REASON = "Sorry, we could not approve your submission as you did not correctly answer one or more validation question(s).";

	private MTurkAPIUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static MTurkClient getSandboxClient() throws URISyntaxException {
		/*
		 * Use the Amazon Mechanical Turk Sandbox to publish test Human Intelligence Tasks (HITs) without paying any
		 * money. Make sure to sign up for a Sanbox account at https://requestersandbox.mturk.com/ with the same
		 * credentials as your main MTurk account.
		 */
		MTurkClientBuilder builder = MTurkClient.builder();
		builder.endpointOverride(new URI(SANDBOX_ENDPOINT));
		builder.region(Region.US_EAST_1);
		return builder.build();
	}

	public static MTurkClient getProductionClient() throws URISyntaxException {
		MTurkClientBuilder builder = MTurkClient.builder();
		builder.endpointOverride(new URI(PROD_ENDPOINT));
		builder.region(Region.US_EAST_1);
		return builder.build();
	}

	public static String getAccountBalance(MTurkClient client) {
		GetAccountBalanceRequest getBalanceRequest = GetAccountBalanceRequest.builder().build();
		GetAccountBalanceResponse getBalanceResponse = client.getAccountBalance(getBalanceRequest);
		return getBalanceResponse.availableBalance();
	}

	public static ReviewPolicy getAssignmentReviewPolicy(int maxAssignments) {
		PolicyParameter answerKeyParam = getAnswerKeyPolicyParameter();

		PolicyParameter rejectScoreParam = PolicyParameter.builder().key("RejectIfKnownAnswerScoreIsLessThan")
				.values("1").build();
		PolicyParameter rejectReasonParam = PolicyParameter.builder().key("RejectReason").values(AUTO_REJECT_REASON)
				.build();

		PolicyParameter extendScoreParam = PolicyParameter.builder().key("ExtendIfKnownAnswerScoreIsLessThan")
				.values("1").build();
		PolicyParameter extendMaxAssignmentsParam = PolicyParameter.builder().key("ExtendMaximumAssignments")
				.values(Integer.toString(2 * maxAssignments)).build();

		ReviewPolicy.Builder builder = ReviewPolicy.builder();
		builder.policyName("ScoreMyKnownAnswers/2011-09-01");
		builder.parameters(answerKeyParam, rejectScoreParam, rejectReasonParam, extendScoreParam,
				extendMaxAssignmentsParam);
		return builder.build();
	}

	private static PolicyParameter getAnswerKeyPolicyParameter() {
		List<ParameterMapEntry> mapEntries = new ArrayList<>();
		// Placeholder

		PolicyParameter.Builder answerKeyBuilder = PolicyParameter.builder();
		answerKeyBuilder.key("AnswerKey");
		answerKeyBuilder.mapEntries(mapEntries);
		return answerKeyBuilder.build();
	}

	public static List<HIT> getHITs(MTurkClient client, String hitTypeId) {
		ListHiTsRequest listHITsRequest = ListHiTsRequest.builder().build();
		ListHiTsResponse listHITsResponse = client.listHITs(listHITsRequest);
		List<HIT> allHITs = listHITsResponse.hiTs();
		return allHITs.stream().filter(hit -> hit.hitTypeId().equals(hitTypeId)).collect(Collectors.toList());
	}

	public static void deleteHITs(MTurkClient client, String hitTypeId) {
		List<HIT> selectedHITs;
		while (!(selectedHITs = getHITs(client, hitTypeId)).isEmpty()) {
			// Set all HITs of the type to expire now
			for (HIT hit : selectedHITs) {
				UpdateExpirationForHitRequest updateHITRequest = UpdateExpirationForHitRequest.builder()
						.hitId(hit.hitId()).expireAt(Instant.now()).build();
				client.updateExpirationForHIT(updateHITRequest);
			}

			// Delete all HITs of the type
			for (HIT hit : selectedHITs) {
				DeleteHitRequest deleteHITRequest = DeleteHitRequest.builder().hitId(hit.hitId()).build();
				client.deleteHIT(deleteHITRequest);
			}
		}
	}

	public static void approveAssignmentsOfReviewableHITs(MTurkClient client, String hitTypeId) {
		AssignmentsCollector assignmentsCollector = new AssignmentsCollector(client, hitTypeId);
		List<HIT> selectedHITs;
		while (!(selectedHITs = getHITs(client, hitTypeId)).isEmpty()) {
			// Reviewable HITs must be approved or denied before getting deleted
			List<HIT> reviewableHITs = selectedHITs.stream().filter(hit -> hit.hitStatus() == HITStatus.REVIEWABLE)
					.collect(Collectors.toList());
			for (HIT hit : reviewableHITs) {
				HITInfo hitInfo = new HITInfo(hit.hitId(), hitTypeId);
				List<Assignment> assignments = assignmentsCollector.collectHITAssignments(hitInfo,
						AssignmentStatus.SUBMITTED);
				assignmentsCollector.approveAssignments(assignments);
			}
		}
	}
}
