package mobilerobot.study.mturk;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.MTurkClientBuilder;
import software.amazon.awssdk.services.mturk.model.Assignment;
import software.amazon.awssdk.services.mturk.model.AssignmentStatus;
import software.amazon.awssdk.services.mturk.model.Comparator;
import software.amazon.awssdk.services.mturk.model.CreateQualificationTypeRequest;
import software.amazon.awssdk.services.mturk.model.CreateQualificationTypeResponse;
import software.amazon.awssdk.services.mturk.model.DeleteHitRequest;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceRequest;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceResponse;
import software.amazon.awssdk.services.mturk.model.HIT;
import software.amazon.awssdk.services.mturk.model.HITAccessActions;
import software.amazon.awssdk.services.mturk.model.HITStatus;
import software.amazon.awssdk.services.mturk.model.ListHiTsRequest;
import software.amazon.awssdk.services.mturk.model.ListHiTsResponse;
import software.amazon.awssdk.services.mturk.model.ListQualificationTypesRequest;
import software.amazon.awssdk.services.mturk.model.ListQualificationTypesResponse;
import software.amazon.awssdk.services.mturk.model.Locale;
import software.amazon.awssdk.services.mturk.model.ParameterMapEntry;
import software.amazon.awssdk.services.mturk.model.PolicyParameter;
import software.amazon.awssdk.services.mturk.model.QualificationRequirement;
import software.amazon.awssdk.services.mturk.model.QualificationType;
import software.amazon.awssdk.services.mturk.model.QualificationTypeStatus;
import software.amazon.awssdk.services.mturk.model.ReviewPolicy;
import software.amazon.awssdk.services.mturk.model.UpdateExpirationForHitRequest;

public class MTurkAPIUtils {

	private static final String SANDBOX_ENDPOINT = "https://mturk-requester-sandbox.us-east-1.amazonaws.com";
	private static final String PROD_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com";

	private static final String CONSENT_QUAL_NAME = "Consent Form for Participation in Research: Understanding mobile robot navigation planning";
	private static final String CONSENT_QUAL_DESCRIPTION = "This is a consent form for participation in research for our study, entitled \"Understanding mobile robot navigation planning\".";
	private static final String CONSENT_QUAL_KEYWORDS = "Consent form, consent form for participation in research";
	private static final long CONSENT_QUAL_DURATION = 5 * 60L; // 5 minutes

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

	public static QualificationRequirement getLocaleRequirement() {
		// QualificationRequirement: Locale IN (US, CA)
		QualificationRequirement.Builder builder = QualificationRequirement.builder();
		builder.qualificationTypeId("00000000000000000071");
		builder.comparator(Comparator.IN);
		Locale usLocale = Locale.builder().country("US").build();
		Locale caLocale = Locale.builder().country("CA").build();
		builder.localeValues(usLocale, caLocale);
		builder.actionsGuarded(HITAccessActions.DISCOVER_PREVIEW_AND_ACCEPT);
		return builder.build();
	}

	public static QualificationRequirement createConsentRequirement(MTurkClient client)
			throws IOException, URISyntaxException {
		File consentFormFile = FileIOUtils.getFile(MTurkAPIUtils.class, "consent-form", "consent-form.xml");
		File answerKeyFile = FileIOUtils.getFile(MTurkAPIUtils.class, "consent-form", "answer-key.xml");
		QualificationType consentQualType = createConsentQualificationType(client, consentFormFile, answerKeyFile);
		QualificationRequirement.Builder builder = QualificationRequirement.builder();
		builder.qualificationTypeId(consentQualType.qualificationTypeId());
		builder.comparator(Comparator.EQUAL_TO);
		builder.integerValues(100); // 100% answer score
		return builder.build();
	}

	private static QualificationType createConsentQualificationType(MTurkClient client, File consentFormFile,
			File answerKeyFile) throws IOException {
		// Check if consent-form Qualification Type has already been created
		ListQualificationTypesRequest listQualTypesRequest = ListQualificationTypesRequest.builder()
				.query(CONSENT_QUAL_NAME).mustBeRequestable(Boolean.FALSE).mustBeOwnedByCaller(Boolean.TRUE).build();
		ListQualificationTypesResponse listQualTypesResponse = client.listQualificationTypes(listQualTypesRequest);
		if (listQualTypesResponse.numResults() > 0) {
			return listQualTypesResponse.qualificationTypes().get(0);
		}

		// Consent-form Qualification-Type has not been created yet
		String consentFormTest = new String(Files.readAllBytes(consentFormFile.toPath()));
		String answerKey = new String(Files.readAllBytes(answerKeyFile.toPath()));

		CreateQualificationTypeRequest.Builder builder = CreateQualificationTypeRequest.builder();
		builder.name(CONSENT_QUAL_NAME);
		builder.description(CONSENT_QUAL_DESCRIPTION);
		builder.keywords(CONSENT_QUAL_KEYWORDS);
		builder.qualificationTypeStatus(QualificationTypeStatus.ACTIVE);
		builder.test(consentFormTest);
		builder.answerKey(answerKey);
		builder.testDurationInSeconds(CONSENT_QUAL_DURATION);
		builder.autoGranted(false);

		CreateQualificationTypeRequest request = builder.build();
		CreateQualificationTypeResponse response = client.createQualificationType(request);
		return response.qualificationType();
	}

	public static ReviewPolicy getAssignmentReviewPolicy() {
		PolicyParameter answerKeyParam = getAnswerKeyPolicyParameter();

		PolicyParameter rejectScoreParam = PolicyParameter.builder().key("RejectIfKnownAnswerScoreIsLessThan")
				.values("1").build();
		PolicyParameter rejectReasonParam = PolicyParameter.builder().key("RejectReason").values(AUTO_REJECT_REASON)
				.build();

		PolicyParameter extendScoreParam = PolicyParameter.builder().key("ExtendIfKnownAnswerScoreIsLessThan")
				.values("1").build();
		PolicyParameter extendMaxAssignmentsParam = PolicyParameter.builder().key("ExtendMaximumAssignments")
				.values(Integer.toString(2 * HITPublisher.MAX_ASSIGNMENTS)).build();

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
