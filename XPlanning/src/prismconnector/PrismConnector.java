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
	private boolean mUseExplicitModel;
	private PrismAPIWrapper mPrismAPI;
	private Map<Policy, Double> mCachedTotalCosts = new HashMap<>();
	private Map<Policy, Map<IQFunction, Double>> mCachedQAValues = new HashMap<>();
	private Map<PrismExplicitModelPointer, Policy> mExplicitModelPtrToPolicy = new HashMap<>();

	public PrismConnector(XMDP xmdp, String outputPath, boolean useExplicitModel) throws PrismException {
		mXMDP = xmdp;
		mOutputPath = outputPath;
		mUseExplicitModel = useExplicitModel;
		mPrismAPI = new PrismAPIWrapper();
	}

	/**
	 * Generate an optimal policy (the objective is the cost function only). Cache its expected total cost and its QA
	 * values.
	 * 
	 * @return An optimal policy.
	 * @throws VarNotFoundException
	 * @throws EffectClassNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws ActionNotFoundException
	 * @throws IncompatibleActionException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws ActionDefinitionNotFoundException
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 * @throws QFunctionNotFoundException
	 */
	public Policy generateOptimalPolicy() throws VarNotFoundException, EffectClassNotFoundException,
			AttributeNameNotFoundException, IncompatibleVarException, DiscriminantNotFoundException,
			ActionNotFoundException, IncompatibleActionException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException, PrismException,
			ResultParsingException, IOException, QFunctionNotFoundException {
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(mXMDP, true, PrismRewardType.STATE_REWARD);

		// If we want to use the PRISM output explicit DTMC model to calculate QA values of the policy, then we need to
		// include QA functions in the MDP translation.
		String mdp = mUseExplicitModel ? mdpTranslator.getMDPTranslationWithQAs() : mdpTranslator.getMDPTranslation();

		// Goal with cost-minimizing objective
		String goalProperty = mdpTranslator.getGoalPropertyTranslation();

		// If mdpTranslator.getMDPTranslationWithQAs() is used, then reward structures include 1 for the cost function
		// and 1 for each of the QA functions.
		// Otherwise, mdpTranslator.getMDPTranslation() is used; there is only 1 reward structure for the cost function.
		int numRewardStructs = mUseExplicitModel ? mXMDP.getQFunctions().size() + 1 : 1;

		// Compute an optimal policy, and cache its total cost and QA values
		return computeOptimalPolicy(mdpTranslator, mdp, goalProperty, true, numRewardStructs);
	}

	/**
	 * Generate an optimal policy w.r.t. a given objective function, that satisfies a given constraint. Cache its QA
	 * values.
	 * 
	 * @param objectiveFunction
	 *            : Objective function
	 * @param constraint
	 *            : Constraint on the expected total value of a particular QA
	 * @return An optimal, constraint-satisfying policy
	 * @throws VarNotFoundException
	 * @throws EffectClassNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws ActionNotFoundException
	 * @throws IncompatibleActionException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws ActionDefinitionNotFoundException
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 * @throws QFunctionNotFoundException
	 */
	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction> constraint) throws VarNotFoundException, EffectClassNotFoundException,
			AttributeNameNotFoundException, IncompatibleVarException, DiscriminantNotFoundException,
			ActionNotFoundException, IncompatibleActionException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException, PrismException,
			ResultParsingException, IOException, QFunctionNotFoundException {
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(mXMDP, true, PrismRewardType.STATE_REWARD);
		PrismRewardTranslator rewardTranslator = mdpTranslator.getPrismRewardTranslator();
		PrismPropertyTranslator propTranslator = mdpTranslator.getPrismPropertyTransltor();
		StringBuilder mdpBuilder = new StringBuilder();
		int numRewardStructs = 0;

		if (mUseExplicitModel) {
			// MDP translation includes all QA functions
			mdpBuilder.append(mdpTranslator.getMDPTranslationWithQAs());

			// 1 reward structure for each of the QA functions, 1 for the cost function
			numRewardStructs += mXMDP.getQFunctions().size() + 1;
		} else {
			// MDP translation includes only the QA function of the value to be constrained
			mdpBuilder.append(mdpTranslator.getMDPTranslation());
			mdpBuilder.append("\n\n");
			mdpBuilder.append(rewardTranslator.getQAFunctionTranslation(constraint.getQFunction()));

			// 1 reward structure for the cost function, 1 for the QA function
			numRewardStructs += 2;
		}

		String objectiveReward = rewardTranslator.getObjectiveFunctionTranslation(objectiveFunction);
		mdpBuilder.append("\n\n");
		mdpBuilder.append(objectiveReward);

		// 1 reward structure for the objective function
		numRewardStructs += 1;

		String mdp = mdpBuilder.toString();
		String property = propTranslator.buildMDPConstrainedMinProperty(mXMDP.getGoal(), objectiveFunction,
				constraint);

		// Compute an optimal policy that satisfies the constraint, and cache its total cost and QA values
		return computeOptimalPolicy(mdpTranslator, mdp, property, false, numRewardStructs);
	}

	/**
	 * Helper method to compute an optimal policy. Cache the policy's expected total cost and QA values.
	 * 
	 * @param mdpTranslator
	 * @param mdpStr
	 *            : MDP string with reward structure(s)
	 * @param propertyString
	 *            : Property string for either minimizing the cost function or other objective function
	 * @param isCostMinProperty
	 *            : Whether the property is minimizing the cost function
	 * @param numRewardStructs
	 *            : Number of reward structures in the MDP
	 * @return An optimal policy
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 * @throws VarNotFoundException
	 * @throws QFunctionNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 * @throws EffectClassNotFoundException
	 * @throws IncompatibleVarException
	 * @throws ActionNotFoundException
	 * @throws DiscriminantNotFoundException
	 * @throws IncompatibleActionException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws AttributeNameNotFoundException
	 */
	private Policy computeOptimalPolicy(PrismMDPTranslator mdpTranslator, String mdpStr, String propertyString,
			boolean isCostMinProperty, int numRewardStructs) throws PrismException, ResultParsingException, IOException,
			VarNotFoundException, QFunctionNotFoundException, ActionDefinitionNotFoundException,
			EffectClassNotFoundException, IncompatibleVarException, ActionNotFoundException,
			DiscriminantNotFoundException, IncompatibleActionException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, AttributeNameNotFoundException {
		// Create explicit model pointer to output directory
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(mOutputPath,
				STA_OUTPUT_FILENAME, TRA_OUTPUT_FILENAME, LAB_OUTPUT_FILENAME, SREW_OUTPUT_FILENAME, numRewardStructs);

		// Create explicit model reader of the output model
		PrismExplicitModelReader explicitModelReader = new PrismExplicitModelReader(
				mdpTranslator.getValueEncodingScheme(), outputExplicitModelPointer);

		// Expected total objective value of the policy -- the objective function is specified in the property
		// The objective function can be the cost function
		double result = mPrismAPI.generateMDPAdversary(mdpStr, propertyString, outputExplicitModelPointer);

		// Read policy from the PRISM output explicit model
		Policy policy = explicitModelReader.readPolicyFromFiles();

		// Map the explicit model pointer to the corresponding policy object
		mExplicitModelPtrToPolicy.put(outputExplicitModelPointer, policy);

		if (isCostMinProperty) {
			// The objective function in the property is the cost function
			// Cache the expected total cost of the policy
			mCachedTotalCosts.put(policy, result);
		} else {
			// The objective function in the property is not the cost function
			// Calculate the expected total cost of the policy from the explicit model, and cache it
			int costStructIndex = mdpTranslator.getValueEncodingScheme().getCostStructureIndex();
			String rawRewardQueryStr = mdpTranslator.getPrismPropertyTransltor()
					.buildDTMCRawRewardQueryProperty(mXMDP.getGoal());
			double totalCost = mPrismAPI.queryPropertyFromExplicitDTMC(rawRewardQueryStr, outputExplicitModelPointer,
					costStructIndex);
			mCachedTotalCosts.put(policy, totalCost);
		}

		if (mUseExplicitModel) {
			// Compute and cache the QA values of the policy, using explicit DTMC model
			computeQAValuesFromExplicitDTMC(outputExplicitModelPointer, mXMDP.getQFunctions(), mdpTranslator);
		} else {
			// Compute and cache the QA values of the policy
			computeQAValues(policy, mXMDP.getQFunctions());
		}

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

	private void computeQAValuesFromExplicitDTMC(PrismExplicitModelPointer explicitDTMCPointer,
			Set<IQFunction> qFunctions, PrismMDPTranslator mdpTranslator)
			throws VarNotFoundException, PrismException, ResultParsingException, QFunctionNotFoundException {
		PrismPropertyTranslator propertyTranslator = mdpTranslator.getPrismPropertyTransltor();
		ValueEncodingScheme encodings = mdpTranslator.getValueEncodingScheme();
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
