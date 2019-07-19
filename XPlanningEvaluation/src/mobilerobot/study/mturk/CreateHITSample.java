package mobilerobot.study.mturk;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import mobilerobot.utilities.FileIOUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.MTurkClientBuilder;
import software.amazon.awssdk.services.mturk.model.Comparator;
import software.amazon.awssdk.services.mturk.model.CreateHitRequest;
import software.amazon.awssdk.services.mturk.model.CreateHitResponse;
import software.amazon.awssdk.services.mturk.model.HITAccessActions;
import software.amazon.awssdk.services.mturk.model.Locale;
import software.amazon.awssdk.services.mturk.model.QualificationRequirement;

public class CreateHITSample {

	/*
	 * Before connecting to MTurk, set up your AWS account and IAM settings as described here:
	 * https://blog.mturk.com/how-to-use-iam-to-control-api-access-to-your-mturk-account-76fe2c2e66e2
	 * 
	 * Configure your AWS credentials as described here:
	 * http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
	 *
	 */

	private static final String SANDBOX_ENDPOINT = "mturk-requester-sandbox.us-east-1.amazonaws.com";
	private static final String PROD_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com";

	private static final String QUESTION_XML_FILE_NAME = "my_question.xml";

	private final MTurkClient mClient;

	public CreateHITSample(final MTurkClient client) {
		mClient = client;
	}

	public HITInfo createHIT(final File questionXMLFile) throws IOException {
		// Read the question XML into a String
		String questionSample = new String(Files.readAllBytes(questionXMLFile.toPath()));

		CreateHitRequest createHitRequest = getCreateHITRequest(questionSample);
		CreateHitResponse response = mClient.createHIT(createHitRequest);

		return new HITInfo(response.hit().hitId(), response.hit().hitTypeId());
	}

	public QualificationRequirement getLocaleRequirement() {
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

	public CreateHitRequest getCreateHITRequest(String question) {
		CreateHitRequest.Builder builder = CreateHitRequest.builder();
		builder.maxAssignments(10);
		builder.lifetimeInSeconds(600L);
		builder.assignmentDurationInSeconds(600L);
		// Reward is a USD dollar amount - USD$0.20 in the example below
		builder.reward("0.20");
		builder.title("Answer a simple question");
		builder.keywords("question, answer, research");
		builder.description("Answer a simple question");
		builder.question(question);
		builder.qualificationRequirements(getLocaleRequirement());
		return builder.build();
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

	public static MTurkClient getProdClient() throws URISyntaxException {
		MTurkClientBuilder builder = MTurkClient.builder();
		builder.endpointOverride(new URI(PROD_ENDPOINT));
		builder.region(Region.US_EAST_1);
		return builder.build();
	}

	public static void main(String[] args) throws URISyntaxException, IOException {
		/*
		 * Use the Amazon Mechanical Turk Sandbox to publish test Human Intelligence Tasks (HITs) without paying any
		 * money. Sign up for a Sandbox account at https://requestersandbox.mturk.com/ with the same credentials as your
		 * main MTurk account
		 * 
		 * Switch to getProdClient() in production. Uncomment line 60, 61, & 66 below to create your HIT in production.
		 * 
		 */
		File questionXMLFile = new File(FileIOUtils.getResourceDir(CreateHITSample.class, "mturk"),
				QUESTION_XML_FILE_NAME);

		final CreateHITSample sandboxApp = new CreateHITSample(getSandboxClient());
		final HITInfo hitInfo = sandboxApp.createHIT(questionXMLFile);

		System.out.println("Your HIT has been created. You can see it at this link:");

		System.out.println("https://workersandbox.mturk.com/mturk/preview?groupId=" + hitInfo.getHITTypeId());
		// System.out.println("https://www.mturk.com/mturk/preview?groupId=" + hitInfo.getHITTypeId());

		System.out.println("Your HIT ID is: " + hitInfo.getHITId());
	}
}
