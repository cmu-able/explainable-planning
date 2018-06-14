package prismconnector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import dtmc.XDTMC;
import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import exceptions.QFunctionNotFoundException;
import exceptions.ResultParsingException;
import exceptions.VarNotFoundException;
import mdp.State;
import mdp.XMDP;
import metrics.IQFunction;
import policy.Policy;
import prism.PrismException;

public class PrismConnector {

	private static final String STA_OUTPUT_FILENAME = "adv.sta";
	private static final String TRA_OUTPUT_FILENAME = "adv.tra";
	private static final String LAB_OUTPUT_FILENAME = "adv.lab";
	private static final String SREW_OUTPUT_FILENAME = "adv.srew";

	private XMDP mXMDP;
	private PrismAPIWrapper mPrismAPI;
	private Map<Policy, Double> mCachedTotalCosts = new HashMap<>();
	private Map<Policy, Map<IQFunction, Double>> mCachedQAValues = new HashMap<>();

	public PrismConnector(XMDP xmdp) throws PrismException {
		mXMDP = xmdp;
		mPrismAPI = new PrismAPIWrapper();
	}

	public Policy generateOptimalPolicy(XMDP xmdp, String outputPath)
			throws VarNotFoundException, EffectClassNotFoundException, AttributeNameNotFoundException,
			IncompatibleVarException, DiscriminantNotFoundException, ActionNotFoundException,
			IncompatibleActionException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException,
			ActionDefinitionNotFoundException, PrismException, ResultParsingException, IOException {
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true, PrismRewardType.STATE_REWARD);
		String mdpWithQAs = mdpTranslator.getMDPTranslationWithQAs();
		String goalProperty = mdpTranslator.getGoalPropertyTranslation();

		// Reward structures include 1 for the cost function and 1 for each of the QA functions
		// Because mdpTranslator.getMDPTranslationWithQAs() is used
		int numRewardStructs = xmdp.getQFunctions().size() + 1;
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath,
				STA_OUTPUT_FILENAME, TRA_OUTPUT_FILENAME, LAB_OUTPUT_FILENAME, SREW_OUTPUT_FILENAME, numRewardStructs);

		double totalCost = mPrismAPI.generateMDPAdversary(mdpWithQAs, goalProperty, outputExplicitModelPointer);

		PrismExplicitModelReader explicitModelReader = new PrismExplicitModelReader(
				mdpTranslator.getValueEncodingScheme(), outputPath);
		Map<Integer, State> stateIndices = explicitModelReader.readStatesFromFile(STA_OUTPUT_FILENAME);
		Policy policy = explicitModelReader.readPolicyFromFile(TRA_OUTPUT_FILENAME, stateIndices);

		// Cache the expected total cost of the policy
		mCachedTotalCosts.put(policy, totalCost);

		// Compute and cache the QA values of the policy
		computeQAValues(policy, mXMDP.getQFunctions());

		return policy;
	}

	public double getExpectedTotalCost(Policy policy) {
		return mCachedTotalCosts.get(policy);
	}

	public double getQAValue(Policy policy, IQFunction qFunction) {
		return mCachedQAValues.get(policy).get(qFunction);
	}

	private void computeQAValues(Policy policy, Set<IQFunction> qFunctions)
			throws ActionDefinitionNotFoundException, EffectClassNotFoundException, VarNotFoundException,
			IncompatibleVarException, ActionNotFoundException, DiscriminantNotFoundException,
			IncompatibleActionException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException,
			AttributeNameNotFoundException, PrismException, ResultParsingException {
		XDTMC xdtmc = new XDTMC(mXMDP, policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslationWithQAs();

		Map<IQFunction, String> queryProperties = new HashMap<>();
		StringBuilder builder = new StringBuilder();
		for (IQFunction qFunction : qFunctions) {
			String queryProperty = dtmcTranslator.getNumQueryPropertyTranslation(qFunction);
			queryProperties.put(qFunction, queryProperty);
			builder.append(queryProperty);
			builder.append("\n");
		}
		String propertiesStr = builder.toString();

		Map<String, Double> results = mPrismAPI.queryPropertiesFromDTMC(dtmcWithQAs, propertiesStr);

		// Cache the QA values of the policy
		Map<IQFunction, Double> qaValues = new HashMap<>();
		for (Entry<IQFunction, String> entry : queryProperties.entrySet()) {
			IQFunction qFunction = entry.getKey();
			String queryProperty = entry.getValue();
			qaValues.put(qFunction, results.get(queryProperty));
		}
		mCachedQAValues.put(policy, qaValues);
	}

	public double computeQAValueFromExplicitDTMC(PrismExplicitModelPointer explicitDTMCPointer, IQFunction qFunction,
			PrismDTMCTranslator dtmcTranslator)
			throws VarNotFoundException, PrismException, ResultParsingException, QFunctionNotFoundException {
		ValueEncodingScheme encodings = dtmcTranslator.getValueEncodingScheme();
		PrismPropertyTranslator propertyTranslator = new PrismPropertyTranslator(encodings);
		String rawRewardQuery = propertyTranslator.buildDTMCRawRewardQueryProperty(mXMDP.getGoal());
		Integer rewardStructIndex = encodings.getRewardStructureIndex(qFunction);
		return mPrismAPI.queryPropertyFromExplicitDTMC(rawRewardQuery, explicitDTMCPointer, rewardStructIndex);
	}
}
