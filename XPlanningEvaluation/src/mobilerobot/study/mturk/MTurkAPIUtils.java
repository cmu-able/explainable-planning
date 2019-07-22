package mobilerobot.study.mturk;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.json.simple.parser.ParseException;

import mobilerobot.study.prefalign.LinkedPrefAlignQuestions;
import mobilerobot.study.utilities.QuestionUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.MTurkClientBuilder;
import software.amazon.awssdk.services.mturk.model.Comparator;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceRequest;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceResponse;
import software.amazon.awssdk.services.mturk.model.HITAccessActions;
import software.amazon.awssdk.services.mturk.model.Locale;
import software.amazon.awssdk.services.mturk.model.ParameterMapEntry;
import software.amazon.awssdk.services.mturk.model.PolicyParameter;
import software.amazon.awssdk.services.mturk.model.QualificationRequirement;
import software.amazon.awssdk.services.mturk.model.ReviewPolicy;

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

	public static ReviewPolicy getAssignmentReviewPolicy(LinkedPrefAlignQuestions linkedQuestions,
			Set<String> validationQuestionDocNames) throws IOException, ParseException {
		PolicyParameter answerKeyParam = getAnswerKeyPolicyParameter(linkedQuestions, validationQuestionDocNames);

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

	private static PolicyParameter getAnswerKeyPolicyParameter(LinkedPrefAlignQuestions linkedQuestions,
			Set<String> easyQuestionDocNames) throws IOException, ParseException {
		List<ParameterMapEntry> mapEntries = new ArrayList<>();
		for (int i = 0; i < linkedQuestions.getNumQuestions(); i++) {
			String questionDocName = linkedQuestions.getQuestionDocumentName(i, false);

			if (easyQuestionDocNames.contains(questionDocName)) {
				File questionDir = linkedQuestions.getQuestionDir(i);
				int agentIndex = linkedQuestions.getQuestionAgentIndex(i);

				// "yes" or "no" answer
				String answer = QuestionUtils.getAnswer(questionDir, agentIndex);

				// "question[i]-answer"
				String questionID = String.format(MTurkHTMLQuestionUtils.QUESTION_ID_FORMAT, i, "answer");
				ParameterMapEntry.Builder mapEntryBuilder = ParameterMapEntry.builder();
				mapEntryBuilder.key(questionID);
				mapEntryBuilder.values(answer);

				mapEntries.add(mapEntryBuilder.build());
			}
		}

		PolicyParameter.Builder answerKeyBuilder = PolicyParameter.builder();
		answerKeyBuilder.key("AnswerKey");
		answerKeyBuilder.mapEntries(mapEntries);
		return answerKeyBuilder.build();
	}
}
