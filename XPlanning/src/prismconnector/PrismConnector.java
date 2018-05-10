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
import exceptions.ResultParsingException;
import exceptions.VarNotFoundException;
import mdp.State;
import mdp.XMDP;
import metrics.IQFunction;
import policy.Policy;
import prism.PrismException;

public class PrismConnector {

	private XMDP mXMDP;
	private PrismAPIWrapper mPrismAPI;
	private Map<Policy, Double> mCachedTotalCosts = new HashMap<>();
	private Map<Policy, Map<IQFunction, Double>> mCachedQAValues = new HashMap<>();

	public PrismConnector(XMDP xmdp) throws PrismException {
		mXMDP = xmdp;
		mPrismAPI = new PrismAPIWrapper();
	}

	public Policy generateOptimalPolicy(XMDP xmdp)
			throws VarNotFoundException, EffectClassNotFoundException, AttributeNameNotFoundException,
			IncompatibleVarException, DiscriminantNotFoundException, ActionNotFoundException,
			IncompatibleActionException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException,
			ActionDefinitionNotFoundException, PrismException, ResultParsingException, IOException {
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true);
		String mdpWithQAs = mdpTranslator.getMDPTranslationWithQAs();
		String goalProperty = mdpTranslator.getGoalPropertyTranslation();

		String outputPath = "/Users/rsukkerd/Projects/explainable-planning/models/test0/output";
		String staOutputFilename = "adv.sta";
		String traOutputFilename = "adv.tra";
		String labOutputFilename = "adv.lab";
		String srewOutputFilename = "adv.srew";
		// Reward structures include 1 for the cost function and 1 for each of the QA functions
		// Because mdpTranslator.getMDPTranslationWithQAs() is used
		int numRewardStructs = xmdp.getQFunctions().size() + 1;
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath,
				staOutputFilename, traOutputFilename, labOutputFilename, srewOutputFilename, numRewardStructs);

		double totalCost = mPrismAPI.generateMDPAdversary(mdpWithQAs, goalProperty, outputExplicitModelPointer);

		PrismExplicitModelReader explicitModelReader = new PrismExplicitModelReader(
				mdpTranslator.getValueEncodingScheme(), outputPath);
		Map<Integer, State> stateIndices = explicitModelReader.readStatesFromFile(staOutputFilename);
		Policy policy = explicitModelReader.readPolicyFromFile(traOutputFilename, stateIndices);

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
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true);
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
}
