package mobilerobot.study.mturk;

import java.net.URISyntaxException;

import software.amazon.awssdk.services.mturk.MTurkClient;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceRequest;
import software.amazon.awssdk.services.mturk.model.GetAccountBalanceResponse;

public class GetAccountBalanceSample {

	/*
	 * Before connecting to MTurk, set up your AWS account and IAM settings as described here:
	 * https://blog.mturk.com/how-to-use-iam-to-control-api-access-to-your-mturk-account-76fe2c2e66e2
	 * 
	 * Configure your AWS credentials as described here:
	 * http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html
	 *
	 */

	private final MTurkClient mClient;

	public GetAccountBalanceSample(final MTurkClient client) {
		mClient = client;
	}

	public String getAccountBalance() {
		GetAccountBalanceRequest getBalanceRequest = GetAccountBalanceRequest.builder().build();
		GetAccountBalanceResponse getBalanceResponse = mClient.getAccountBalance(getBalanceRequest);
		return getBalanceResponse.availableBalance();
	}

	public static void main(String[] args) throws URISyntaxException {
		/*
		 * Use the Amazon Mechanical Turk Sandbox to publish test Human Intelligence Tasks (HITs) without paying any
		 * money. Sign up for a Sandbox account at https://requestersandbox.mturk.com/ with the same credentials as your
		 * main MTurk account.
		 */
		final GetAccountBalanceSample sandboxApp = new GetAccountBalanceSample(CreateHITSample.getSandboxClient());
		final String sandboxBalance = sandboxApp.getAccountBalance();

		// In Sandbox this will always return $10,000
		System.out.println("SANDBOX - Your account balance is " + sandboxBalance);

		// Connect to the live marketplace and get your real account balance
		// final GetAccountBalanceSample productionApp = new GetAccountBalanceSample(CreateHITSample.getProdClient());
		// final String productionBalance = productionApp.getAccountBalance();
		// System.out.println("PRODUCTION - Your account balance is " + productionBalance);
	}
}
