package mobilerobot.study.mturk;

import java.net.URI;
import java.net.URISyntaxException;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.MTurkClientBuilder;
import software.amazon.awssdk.services.mturk.model.Comparator;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceRequest;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceResponse;
import software.amazon.awssdk.services.mturk.model.HITAccessActions;
import software.amazon.awssdk.services.mturk.model.Locale;
import software.amazon.awssdk.services.mturk.model.QualificationRequirement;

public class MTurkAPIUtils {

	private static final String SANDBOX_ENDPOINT = "https://mturk-requester-sandbox.us-east-1.amazonaws.com";
	private static final String PROD_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com";

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
}
