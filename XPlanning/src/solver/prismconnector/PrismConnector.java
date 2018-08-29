package solver.prismconnector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import explanation.analysis.EventBasedQAValue;
import language.dtmc.XDTMC;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.metrics.IEvent;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;
import language.metrics.NonStandardMetricQFunction;
import language.objectives.AttributeConstraint;
import language.objectives.IAdditiveCostFunction;
import language.policy.Policy;
import language.qfactors.IAction;
import prism.PrismException;
import solver.prismconnector.PrismConfiguration.PrismEngine;
import solver.prismconnector.PrismConfiguration.PrismMDPMultiSolutionMethod;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PolicyReader;

public class PrismConnector {

	public static final String DEFAULT_MODEL_FILENAME_PREFIX = "model";

	private XMDP mXMDP;
	private PrismMDPTranslator mMDPTranslator;
	private PrismConnectorSettings mSettings;
	private PrismAPIWrapper mPrismAPI;
	private Map<Policy, Double> mCachedTotalCosts = new HashMap<>();
	private Map<Policy, Map<IQFunction<?, ?>, Double>> mCachedQAValues = new HashMap<>();
	private Map<PrismExplicitModelPointer, Policy> mExplicitModelPtrToPolicy = new HashMap<>();

	public PrismConnector(XMDP xmdp, PrismConnectorSettings settings) throws PrismException {
		mXMDP = xmdp;
		mMDPTranslator = new PrismMDPTranslator(xmdp, true, PrismRewardType.STATE_REWARD);
		mSettings = settings;
		mPrismAPI = new PrismAPIWrapper(settings.getPrismConfiguration());
	}

	public XMDP getXMDP() {
		return mXMDP;
	}

	/**
	 * Generate an optimal policy (the objective is the cost function only). Compute its QA values. Cache its expected
	 * total cost and QA values.
	 * 
	 * @return An optimal policy, if exists.
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 */
	public Policy generateOptimalPolicy() throws XMDPException, PrismException, ResultParsingException, IOException {
		// If we want to use the PRISM output explicit DTMC model to calculate QA values of the policy, then we need to
		// include QA functions in the MDP translation.
		String mdp = mMDPTranslator.getMDPTranslation(mSettings.useExplicitModel());

		// Goal with cost-minimizing objective
		String goalProperty = mMDPTranslator.getGoalPropertyTranslation();

		// Compute an optimal policy, and cache its total cost and QA values
		return computeOptimalPolicy(mdp, goalProperty, mSettings.getOutputPath());
	}

	/**
	 * Generate an optimal policy w.r.t. a given objective function, that satisfies a given constraint. Compute its QA
	 * values and its expected total cost (not the objective value). Cache its expected total cost and QA values.
	 * 
	 * @param objectiveFunction
	 *            : Objective function
	 * @param constraint
	 *            : Constraint on the expected total value of a particular QA
	 * @return An optimal, constraint-satisfying policy, if exists.
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 */
	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction<?, ?>> constraint)
			throws XMDPException, PrismException, ResultParsingException, IOException {
		configureForMultiObjectiveStrategySynthesis();

		PrismRewardTranslator rewardTranslator = mMDPTranslator.getPrismRewardTranslator();
		PrismPropertyTranslator propTranslator = mMDPTranslator.getPrismPropertyTransltor();

		StringBuilder mdpBuilder = new StringBuilder();
		String originalMDPStr = mMDPTranslator.getMDPTranslation(mSettings.useExplicitModel());
		mdpBuilder.append(originalMDPStr);

		if (mSettings.useExplicitModel()) {
			// MDP translation already includes all QA functions
		} else {
			// MDP translation includes only the QA function of the value to be constrained
			mdpBuilder.append("\n\n");
			mdpBuilder.append(rewardTranslator.getQAFunctionTranslation(constraint.getQFunction()));
		}

		String objectiveReward = rewardTranslator.getObjectiveFunctionTranslation(objectiveFunction);
		mdpBuilder.append("\n\n");
		mdpBuilder.append(objectiveReward);

		String mdpStr = mdpBuilder.toString();
		String propertyStr = propTranslator.buildMDPConstrainedMinProperty(mXMDP.getGoal(), objectiveFunction,
				constraint);

		// Compute an optimal policy that satisfies the constraint, and cache its total cost and QA values
		String outputPath = mSettings.getOutputPath() + "_" + constraint.getQFunction().getName();
		return computeOptimalPolicy(mdpStr, propertyStr, outputPath);
	}

	private void configureForMultiObjectiveStrategySynthesis() {
		// Use transition rewards for multi-objective adversary synthesis
		mMDPTranslator.setPrismRewardType(PrismRewardType.TRANSITION_REWARD);

		// Use Sparse engine and linear-programming solution method for multi-objective adversary synthesis
		mPrismAPI.reconfigurePrism(PrismEngine.SPARSE);
		mPrismAPI.reconfigurePrism(PrismMDPMultiSolutionMethod.LINEAR_PROGRAMMING);
	}

	/**
	 * Helper method to compute an optimal policy. Cache the policy's expected total cost and QA values.
	 * 
	 * @param mdpStr
	 *            : MDP string with reward structure(s)
	 * @param propertyStr
	 *            : Property string for either minimizing the cost function or other objective function
	 * @param outputPath
	 *            : Output path for PRISM explicit model files
	 * @return An optimal policy, if exists.
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 * @throws XMDPException
	 */
	private Policy computeOptimalPolicy(String mdpStr, String propertyStr, String outputPath)
			throws PrismException, ResultParsingException, IOException, XMDPException {
		// Create explicit model pointer to output directory
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath,
				DEFAULT_MODEL_FILENAME_PREFIX, mMDPTranslator.getPrismRewardType());

		// Create explicit model reader of the output model
		PolicyReader explicitModelReader = new PolicyReader(
				outputExplicitModelPointer, mMDPTranslator.getValueEncodingScheme());

		// Expected total objective value of the policy -- the objective function is specified in the property
		// The objective function can be the cost function
		double result = mPrismAPI.generateMDPAdversary(mdpStr, propertyStr, outputExplicitModelPointer);

		if (Double.isNaN(result) || Double.isInfinite(result)) {
			// No solution policy found
			return null;
		}

		// Read policy from the PRISM output explicit model
		Policy policy = explicitModelReader.readPolicyFromFiles();

		// Map the explicit model pointer to the corresponding policy object
		mExplicitModelPtrToPolicy.put(outputExplicitModelPointer, policy);

		if (isCostMinProperty(propertyStr)) {
			// The objective function in the property is the cost function
			// Cache the expected total cost of the policy
			mCachedTotalCosts.put(policy, result);
		} else {
			// The objective function in the property is not the cost function
			// Calculate the expected total cost of the policy, and cache it
			computeExpectedTotalCost(policy);
		}

		if (mSettings.useExplicitModel()) {
			// Compute and cache the QA values of the policy, using explicit DTMC model
			computeQAValuesFromExplicitDTMC(outputExplicitModelPointer, mXMDP.getQSpace());
		} else {
			// Compute and cache the QA values of the policy
			computeQAValues(policy, mXMDP.getQSpace());
		}

		return policy;
	}

	/**
	 * 
	 * @param propertyStr
	 * @return Whether the property is minimizing the cost function
	 */
	private boolean isCostMinProperty(String propertyStr) {
		return propertyStr.contains("R{\"cost\"}min=?");
	}

	/**
	 * Retrieve the expected total cost of a given policy from the cache. If the policy is not already in the cache,
	 * then compute and cache its expected total cost.
	 * 
	 * @param policy
	 * @return Expected total cost of the policy
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double getExpectedTotalCost(Policy policy) throws XMDPException, PrismException, ResultParsingException {
		if (!mCachedTotalCosts.containsKey(policy)) {
			computeExpectedTotalCost(policy);
		}
		return mCachedTotalCosts.get(policy);
	}

	private void computeExpectedTotalCost(Policy policy) throws XMDPException, PrismException, ResultParsingException {
		XDTMC xdtmc = new XDTMC(mXMDP, policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
		String dtmc = dtmcTranslator.getDTMCTranslation(false);
		String queryProperty = dtmcTranslator.getCostQueryPropertyTranslation();
		double totalCost = mPrismAPI.queryPropertyFromDTMC(dtmc, queryProperty);
		mCachedTotalCosts.put(policy, totalCost);
	}

	/**
	 * Retrieve the QA value of a given policy from the cache. If the policy is not already in the cache, then compute
	 * and cache all of its QA values.
	 * 
	 * @param policy
	 * @param qFunction
	 *            : QA function
	 * @return QA value of the policy
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double getQAValue(Policy policy, IQFunction<?, ?> qFunction)
			throws XMDPException, PrismException, ResultParsingException {
		if (!mXMDP.getQSpace().contains(qFunction)) {
			throw new QFunctionNotFoundException(qFunction);
		}
		if (!mCachedQAValues.containsKey(policy)) {
			computeQAValues(policy, mXMDP.getQSpace());
		}
		return mCachedQAValues.get(policy).get(qFunction);
	}

	private void computeQAValues(Policy policy, Iterable<IQFunction<IAction, ITransitionStructure<IAction>>> qSpace)
			throws XMDPException, PrismException, ResultParsingException {
		XDTMC xdtmc = new XDTMC(mXMDP, policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);

		Map<IQFunction<?, ?>, String> queryProperties = new HashMap<>();
		StringBuilder builder = new StringBuilder();
		for (IQFunction<?, ?> qFunction : qSpace) {
			String queryProperty = dtmcTranslator.getNumQueryPropertyTranslation(qFunction);
			queryProperties.put(qFunction, queryProperty);
			builder.append(queryProperty);
			builder.append("\n");
		}
		String propertiesStr = builder.toString();

		Map<String, Double> results = mPrismAPI.queryPropertiesFromDTMC(dtmcWithQAs, propertiesStr);

		// Cache the QA values of the policy
		Map<IQFunction<?, ?>, Double> qaValues = new HashMap<>();
		for (Entry<IQFunction<?, ?>, String> entry : queryProperties.entrySet()) {
			IQFunction<?, ?> qFunction = entry.getKey();
			String queryProperty = entry.getValue();
			qaValues.put(qFunction, results.get(queryProperty));
		}
		mCachedQAValues.put(policy, qaValues);
	}

	private void computeQAValuesFromExplicitDTMC(PrismExplicitModelPointer explicitDTMCPointer,
			Iterable<IQFunction<IAction, ITransitionStructure<IAction>>> qSpace)
			throws XMDPException, PrismException, ResultParsingException {
		PrismPropertyTranslator propertyTranslator = mMDPTranslator.getPrismPropertyTransltor();
		ValueEncodingScheme encodings = mMDPTranslator.getValueEncodingScheme();
		String rawRewardQuery = propertyTranslator.buildDTMCRawRewardQueryProperty(mXMDP.getGoal());

		// Cache the QA values of the policy
		Map<IQFunction<?, ?>, Double> qaValues = new HashMap<>();
		for (IQFunction<?, ?> qFunction : qSpace) {
			Integer rewardStructIndex = encodings.getRewardStructureIndex(qFunction);
			double qaValue = mPrismAPI.queryPropertyFromExplicitDTMC(rawRewardQuery, explicitDTMCPointer,
					rewardStructIndex);
			qaValues.put(qFunction, qaValue);
		}
		Policy policy = mExplicitModelPtrToPolicy.get(explicitDTMCPointer);
		mCachedQAValues.put(policy, qaValues);
	}

	/**
	 * Compute the expected total occurrences of each event in a given non-standard QA metric.
	 * 
	 * @param policy
	 * @param qFunction
	 *            : Non-standard QA function
	 * @return Event-based QA value of the policy
	 * @throws XMDPException
	 * @throws ResultParsingException
	 * @throws PrismException
	 */
	public <E extends IEvent<?, ?>> EventBasedQAValue<E> computeEventBasedQAValue(Policy policy,
			NonStandardMetricQFunction<?, ?, E> qFunction)
			throws XMDPException, ResultParsingException, PrismException {
		XDTMC xdtmc = new XDTMC(mXMDP, policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
		String dtmc = dtmcTranslator.getDTMCTranslation(false);
		String eventCounterRewards = dtmcTranslator.getEventCounterRewardsTranslation(qFunction.getEventBasedMetric());

		StringBuilder dtmcStrBuilder = new StringBuilder();
		dtmcStrBuilder.append(dtmc);
		dtmcStrBuilder.append("\n\n");
		dtmcStrBuilder.append(eventCounterRewards);
		String dtmcWithEventCounters = dtmcStrBuilder.toString();

		Map<E, String> eventQueryProps = new HashMap<>();
		StringBuilder propsStrBuilder = new StringBuilder();
		for (E event : qFunction.getEventBasedMetric().getEvents()) {
			String eventQueryProp = dtmcTranslator.getEventCountPropertyTranslation(event);
			eventQueryProps.put(event, eventQueryProp);
			propsStrBuilder.append(eventQueryProp);
			propsStrBuilder.append("\n");
		}
		String propsStr = propsStrBuilder.toString();

		Map<String, Double> results = mPrismAPI.queryPropertiesFromDTMC(dtmcWithEventCounters, propsStr);

		EventBasedQAValue<E> eventBasedQAValue = new EventBasedQAValue<>();
		for (Entry<E, String> entry : eventQueryProps.entrySet()) {
			E event = entry.getKey();
			String eventQueryProp = entry.getValue();
			eventBasedQAValue.putExpectedCount(event, results.get(eventQueryProp));
		}

		return eventBasedQAValue;
	}

	/**
	 * Closing down PRISM. Only invoke this method when finishing using this {@link PrismConnector}.
	 */
	public void terminate() {
		mPrismAPI.terminatePrism();
	}
}
