package mobilerobot.study.mturk;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.Comparator;
import software.amazon.awssdk.services.mturk.model.CreateQualificationTypeRequest;
import software.amazon.awssdk.services.mturk.model.CreateQualificationTypeResponse;
import software.amazon.awssdk.services.mturk.model.ListQualificationTypesRequest;
import software.amazon.awssdk.services.mturk.model.ListQualificationTypesResponse;
import software.amazon.awssdk.services.mturk.model.QualificationRequirement;
import software.amazon.awssdk.services.mturk.model.QualificationType;
import software.amazon.awssdk.services.mturk.model.QualificationTypeStatus;

public class QualificationUtils {

	private static final String TEST_QUAL_NAME = "passed_qualification_tests";
	private static final String TEST_QUAL_DESCRIPTION = "This is a basic arithemtic test and consent form for participation in research for our study, entitled \"Understanding mobile robot navigation planning\".";
	private static final String TEST_QUAL_KEYWORDS = "Arithmetic test, consent form for participation in research";
	private static final Long TEST_QUAL_DURATION = 5 * 60L; // 5 minutes

	private QualificationUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static QualificationRequirement getTestQualificationRequirement(MTurkClient client)
			throws IOException, URISyntaxException {
		File qualTestFormFile = FileIOUtils.getFile(MTurkAPIUtils.class, "qualification", "test-consent-form.xml");
		File answerKeyFile = FileIOUtils.getFile(MTurkAPIUtils.class, "qualification", "answer-key.xml");
		QualificationType consentQualType = createTestQualificationType(client, qualTestFormFile, answerKeyFile);
		QualificationRequirement.Builder builder = QualificationRequirement.builder();
		builder.qualificationTypeId(consentQualType.qualificationTypeId());
		builder.comparator(Comparator.EQUAL_TO);
		builder.integerValues(100); // 100% answer score
		return builder.build();
	}

	private static QualificationType createTestQualificationType(MTurkClient client, File qualTestFormFile,
			File answerKeyFile) throws IOException {
		// Check if this Qualification Type has already been created
		ListQualificationTypesRequest listQualTypesRequest = ListQualificationTypesRequest.builder()
				.query(TEST_QUAL_NAME).mustBeRequestable(Boolean.FALSE).mustBeOwnedByCaller(Boolean.TRUE).build();
		ListQualificationTypesResponse listQualTypesResponse = client.listQualificationTypes(listQualTypesRequest);
		if (listQualTypesResponse.numResults() > 0) {
			return listQualTypesResponse.qualificationTypes().get(0);
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
}
