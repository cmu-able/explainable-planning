package mobilerobot.study.mturk;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.AssociateQualificationWithWorkerRequest;
import software.amazon.awssdk.services.mturk.model.Comparator;
import software.amazon.awssdk.services.mturk.model.CreateQualificationTypeRequest;
import software.amazon.awssdk.services.mturk.model.CreateQualificationTypeResponse;
import software.amazon.awssdk.services.mturk.model.HITAccessActions;
import software.amazon.awssdk.services.mturk.model.ListQualificationTypesRequest;
import software.amazon.awssdk.services.mturk.model.ListQualificationTypesResponse;
import software.amazon.awssdk.services.mturk.model.Locale;
import software.amazon.awssdk.services.mturk.model.QualificationRequirement;
import software.amazon.awssdk.services.mturk.model.QualificationType;
import software.amazon.awssdk.services.mturk.model.QualificationTypeStatus;

public class QualificationUtils {

	private static final String TEST_QUAL_NAME = "passed_4_qualification_tests_consented_to_research";
	private static final String TEST_QUAL_DESCRIPTION = "This is a basic arithemtic test and consent form for participation in research for our study, entitled \"Understanding mobile robot navigation planning\".";
	private static final String TEST_QUAL_KEYWORDS = "Arithmetic test, consent form for participation in research";
	private static final Long TEST_QUAL_DURATION = 10 * 60L; // 10 minutes

	private static final String STAMP_QUAL_NAME = "already_participated";
	private static final String STAMP_QUAL_DESCRIPTION = "Already participated in our study, entitled \"Understanding mobile robot navigation planning\".";

	private static final String MASTERS_QUAL_TYPE_ID_SANDBOX = "2ARFPLSP75KLA8M8DH1HTEQVJT3SY6";
	private static final String MASTERS_QUAL_TYPE_ID_PROD = "2F1QJWKUDD8XADTFD2Q0G6UTO95ALH";

	private QualificationUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Qualification requirement: Locale IN (US, CA).
	 * 
	 * @return Qualification requirement: Locale IN (US, CA)
	 */
	public static QualificationRequirement createLocaleRequirement() {
		QualificationRequirement.Builder builder = QualificationRequirement.builder();
		builder.qualificationTypeId("00000000000000000071");
		builder.comparator(Comparator.IN);
		Locale usLocale = Locale.builder().country("US").build();
		Locale caLocale = Locale.builder().country("CA").build();
		builder.localeValues(usLocale, caLocale);
		builder.actionsGuarded(HITAccessActions.DISCOVER_PREVIEW_AND_ACCEPT);
		return builder.build();
	}

	/**
	 * Worker_​NumberHITsApproved >= 10,000 HITs requirement.
	 * 
	 * @return Worker_​NumberHITsApproved >= 10,000 HITs requirement
	 */
	public static QualificationRequirement createHighNumberHITsApprovedRequirement() {
		QualificationRequirement.Builder builder = QualificationRequirement.builder();
		builder.qualificationTypeId("00000000000000000040"); // Worker_​NumberHITsApproved
		builder.comparator(Comparator.GREATER_THAN_OR_EQUAL_TO);
		builder.integerValues(10000);
		builder.actionsGuarded(HITAccessActions.DISCOVER_PREVIEW_AND_ACCEPT);
		return builder.build();
	}

	/**
	 * Worker_​PercentAssignmentsApproved > 97% requirement.
	 * 
	 * @return Worker_​PercentAssignmentsApproved > 97% requirement
	 */
	public static QualificationRequirement createHighPercentAssignmentsApprovedRequirement() {
		QualificationRequirement.Builder builder = QualificationRequirement.builder();
		builder.qualificationTypeId("000000000000000000L0"); // Worker_​PercentAssignmentsApproved
		builder.comparator(Comparator.GREATER_THAN);
		builder.integerValues(97);
		builder.actionsGuarded(HITAccessActions.DISCOVER_PREVIEW_AND_ACCEPT);
		return builder.build();
	}

	/**
	 * Masters Qualification requirement.
	 * 
	 * @param isProd
	 * @return Masters Qualification requirement
	 */
	public static QualificationRequirement createMastersQualificationRequirement(boolean isProd) {
		QualificationRequirement.Builder builder = QualificationRequirement.builder();
		builder.qualificationTypeId(isProd ? MASTERS_QUAL_TYPE_ID_PROD : MASTERS_QUAL_TYPE_ID_SANDBOX);
		builder.comparator(Comparator.EXISTS);
		builder.actionsGuarded(HITAccessActions.DISCOVER_PREVIEW_AND_ACCEPT);
		return builder.build();
	}

	/**
	 * Qualification requirement: arithmetic test and consent form.
	 * 
	 * @param client
	 * @return Qualification requirement: arithmetic test and consent form
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static QualificationRequirement createTestQualificationRequirement(MTurkClient client)
			throws IOException, URISyntaxException {
		File qualTestFormFile = FileIOUtils.getFile(MTurkAPIUtils.class, "qualification", "test-consent-form.xml");
		File answerKeyFile = FileIOUtils.getFile(MTurkAPIUtils.class, "qualification", "answer-key.xml");
		QualificationType consentQualType = getTestQualificationType(client, qualTestFormFile, answerKeyFile);
		QualificationRequirement.Builder builder = QualificationRequirement.builder();
		builder.qualificationTypeId(consentQualType.qualificationTypeId());
		builder.comparator(Comparator.EQUAL_TO);
		builder.integerValues(100); // 100% answer score
		return builder.build();
	}

	/**
	 * Get or create qualification test: arithmetic test and consent form.
	 * 
	 * @param client
	 * @param qualTestFormFile
	 * @param answerKeyFile
	 * @return Qualification test: arithmetic test and consent form
	 * @throws IOException
	 */
	private static QualificationType getTestQualificationType(MTurkClient client, File qualTestFormFile,
			File answerKeyFile) throws IOException {
		QualificationType testQualType = getExistingQualificationType(client, TEST_QUAL_NAME);
		if (testQualType != null) {
			return testQualType;
		}

		// This Qualification Type has not been created yet
		String qualTestForm = new String(Files.readAllBytes(qualTestFormFile.toPath()));
		String answerKey = new String(Files.readAllBytes(answerKeyFile.toPath()));

		CreateQualificationTypeRequest.Builder builder = CreateQualificationTypeRequest.builder();
		builder.name(TEST_QUAL_NAME);
		builder.description(TEST_QUAL_DESCRIPTION);
		builder.keywords(TEST_QUAL_KEYWORDS);
		builder.qualificationTypeStatus(QualificationTypeStatus.ACTIVE);
		builder.test(qualTestForm);
		builder.answerKey(answerKey);
		builder.testDurationInSeconds(TEST_QUAL_DURATION);
		builder.autoGranted(false);

		CreateQualificationTypeRequest request = builder.build();
		CreateQualificationTypeResponse response = client.createQualificationType(request);
		return response.qualificationType();
	}

	/**
	 * Qualification requirement: first-time participant.
	 * 
	 * @param client
	 * @return Qualification requirement: first-time participant
	 */
	public static QualificationRequirement createFirstParticipationRequirement(MTurkClient client) {
		QualificationType participationStamp = getParticipationStamp(client);
		QualificationRequirement.Builder builder = QualificationRequirement.builder();
		builder.qualificationTypeId(participationStamp.qualificationTypeId());
		builder.comparator(Comparator.DOES_NOT_EXIST);
		builder.actionsGuarded(HITAccessActions.DISCOVER_PREVIEW_AND_ACCEPT);
		return builder.build();
	}

	/**
	 * Get or create Participation Stamp to indicate that a Worker has already participated in the study.
	 * 
	 * @param client
	 * @return Participation Stamp to indicate that a Worker has already participated in the study
	 */
	private static QualificationType getParticipationStamp(MTurkClient client) {
		QualificationType participationStamp = getExistingQualificationType(client, STAMP_QUAL_NAME);
		if (participationStamp != null) {
			return participationStamp;
		}

		// This Qualification Type has not been created yet
		CreateQualificationTypeRequest.Builder builder = CreateQualificationTypeRequest.builder();
		builder.name(STAMP_QUAL_NAME);
		builder.description(STAMP_QUAL_DESCRIPTION);
		builder.qualificationTypeStatus(QualificationTypeStatus.ACTIVE);
		builder.autoGranted(false);

		CreateQualificationTypeRequest request = builder.build();
		CreateQualificationTypeResponse response = client.createQualificationType(request);
		return response.qualificationType();
	}

	private static QualificationType getExistingQualificationType(MTurkClient client, String query) {
		// Check if Qualification Type has already been created
		ListQualificationTypesRequest listQualTypesRequest = ListQualificationTypesRequest.builder().query(query)
				.mustBeRequestable(Boolean.FALSE).mustBeOwnedByCaller(Boolean.TRUE).build();
		ListQualificationTypesResponse listQualTypesResponse = client.listQualificationTypes(listQualTypesRequest);
		if (listQualTypesResponse.numResults() > 0) {
			return listQualTypesResponse.qualificationTypes().get(0);
		}
		return null;
	}

	public static void grantParticipationStampQualification(MTurkClient client, String workerID) {
		QualificationType participationStamp = getParticipationStamp(client);
		AssociateQualificationWithWorkerRequest.Builder builder = AssociateQualificationWithWorkerRequest.builder();
		builder.qualificationTypeId(participationStamp.qualificationTypeId());
		builder.workerId(workerID);
		builder.integerValue(1);
		builder.sendNotification(false);

		AssociateQualificationWithWorkerRequest request = builder.build();
		client.associateQualificationWithWorker(request);
	}
}
