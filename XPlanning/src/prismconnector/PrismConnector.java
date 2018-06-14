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
import objectives.AttributeConstraint;
import objectives.IAdditiveCostFunction;
import policy.Policy;
import prism.PrismException;

public class PrismConnector {

	private static final String STA_OUTPUT_FILENAME = "adv.sta";
	private static final String TRA_OUTPUT_FILENAME = "adv.tra";
	private static final String LAB_OUTPUT_FILENAME = "adv.lab";
	private static final String SREW_OUTPUT_FILENAME = "adv.srew";

	private XMDP mXMDP;
	private String mOutputPath;
	private PrismAPIWrapper mPrismAPI;
	private Map<Policy, Double> mCachedTotalCosts = new HashMap<>();
	private Map<Policy, Map<IQFunction, Double>> mCachedQAValues = new HashMap<>();
	private Map<PrismExplicitModelPointer, Policy> mExplicitModelPtrToPolicy = new HashMap<>();

	public PrismConnector(XMDP xmdp, String outputPath) throws PrismException {
		mXMDP = xmdp;
		mOutputPath = outputPath;
		mPrismAPI = new PrismAPIWrapper();
	}

	public Policy generateOptimalPolicy(boolean useExplicitModel) throws VarNotFoundException,
			EffectClassNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException, ActionNotFoundException, IncompatibleActionException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException,
			PrismException, ResultParsingException, IOException, QFunctionNotFoundException {
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(mXMDP, true, PrismRewardType.STATE_REWARD);

		// If we want to use the PRISM output explicit DTMC model to calculate QA values of the policy, then we need to
		// include QA functions in the MDP translation.
		String mdp = useExplicitModel ? mdpTranslator.getMDPTranslationWithQAs() : mdpTranslator.getMDPTranslation();

		String goalProperty = mdpTranslator.getGoalPropertyTranslation();

		// If mdpTranslator.getMDPTranslationWithQAs() is used, then reward structures include 1 for the cost function
		// and 1 for each of the QA functions.
		// Otherwise, mdpTranslator.getMDPTranslation() is used; there is only 1 reward structure for the cost function.
		int numRewardStructs = useExplicitModel ? mXMDP.getQFunctions().size() + 1 : 1;

		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(mOutputPath,
				STA_OUTPUT_FILENAME, TRA_OUTPUT_FILENAME, LAB_OUTPUT_FILENAME, SREW_OUTPUT_FILENAME, numRewardStructs);

		double totalCost = mPrismAPI.generateMDPAdversary(mdp, goalProperty, outputExplicitModelPointer);

		PrismExplicitModelReader explicitModelReader = new PrismExplicitModelReader(
				mdpTranslator.getValueEncodingScheme(), mOutputPath);
		Map<Integer, State> stateIndices = explicitModelReader.readStatesFromFile(STA_OUTPUT_FILENAME);
		Policy policy = explicitModelReader.readPolicyFromFile(TRA_OUTPUT_FILENAME, stateIndices);

		// Map the explicit model pointer to the corresponding policy object
		mExplicitModelPtrToPolicy.put(outputExplicitModelPointer, policy);

		// Cache the expected total cost of the policy
		mCachedTotalCosts.put(policy, totalCost);

		if (useExplicitModel) {
			// Compute and cache the QA values of the policy, using explicit DTMC model
			computeQAValuesFromExplicitDTMC(outputExplicitModelPointer, mXMDP.getQFunctions(),
					mdpTranslator.getValueEncodingScheme());
		} else {
			// Compute and cache the QA values of the policy
			computeQAValues(policy, mXMDP.getQFunctions());
		}

		return policy;
	}

	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction> constraint) {
		return null;
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

	private void computeQAValuesFromExplicitDTMC(PrismExplicitModelPointer explicitDTMCPointer,
			Set<IQFunction> qFunctions, ValueEncodingScheme encodings)
			throws VarNotFoundException, PrismException, ResultParsingException, QFunctionNotFoundException {
		PrismPropertyTranslator propertyTranslator = new PrismPropertyTranslator(encodings);
		String rawRewardQuery = propertyTranslator.buildDTMCRawRewardQueryProperty(mXMDP.getGoal());

		// Cache the QA values of the policy
		Map<IQFunction, Double> qaValues = new HashMap<>();
		for (IQFunction qFunction : qFunctions) {
			Integer rewardStructIndex = encodings.getRewardStructureIndex(qFunction);
			double qaValue = mPrismAPI.queryPropertyFromExplicitDTMC(rawRewardQuery, explicitDTMCPointer,
					rewardStructIndex);
			qaValues.put(qFunction, qaValue);
		}
		Policy policy = mExplicitModelPtrToPolicy.get(explicitDTMCPointer);
		mCachedQAValues.put(policy, qaValues);
	}
}
