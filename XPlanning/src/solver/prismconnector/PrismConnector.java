package solver.prismconnector;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import explanation.analysis.EventBasedQAValue;
import explanation.analysis.PolicyInfo;
import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.NonStandardMetricQFunction;
import language.domain.models.IAction;
import language.dtmc.XDTMC;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeCostFunction;
import language.objectives.CostCriterion;
import language.objectives.CostFunction;
import language.objectives.IAdditiveCostFunction;
import language.policy.Policy;
import prism.PrismException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class PrismConnector {

	public static final String DEFAULT_MODEL_FILENAME_PREFIX = "model";

	private XMDP mXMDP;
	private CostCriterion mCostCriterion;
	private PrismMDPTranslator mMDPTranslator;
	private PrismConnectorSettings mSettings;
	private PrismAPIWrapper mPrismAPI;
	private Map<Policy, Double> mCachedCosts = new HashMap<>();
	private Map<Policy, Map<IQFunction<?, ?>, Double>> mCachedQAValues = new HashMap<>();
	private Map<Policy, Map<IQFunction<?, ?>, Double>> mCachedQACosts = new HashMap<>();
	private Map<PrismExplicitModelPointer, Policy> mExplicitModelPtrToPolicy = new HashMap<>();

	public PrismConnector(XMDP xmdp, CostCriterion costCriterion, PrismConnectorSettings settings)
			throws PrismException {
		this(xmdp, null, costCriterion, settings);
	}

	public PrismConnector(XMDP xmdp, StateVarTuple queryState, CostCriterion costCriterion,
			PrismConnectorSettings settings) throws PrismException {
		mXMDP = xmdp;
		mCostCriterion = costCriterion;
		mMDPTranslator = new PrismMDPTranslator(xmdp, queryState);
		mSettings = settings;
		mPrismAPI = new PrismAPIWrapper();

		if (costCriterion == CostCriterion.AVERAGE_COST) {
			mPrismAPI.configureForSteadySteadProperty();
		}
	}

	public XMDP getXMDP() {
		return mXMDP;
	}

	public CostCriterion getCostCriterion() {
		return mCostCriterion;
	}

	public PrismMDPTranslator getPrismMDPTranslator() {
		return mMDPTranslator;
	}

	/**
	 * Export the PRISM explicit model files from this XMDP. The explicit model files include: states file (.sta),
	 * transitions file (.tra), labels file (.lab), and transition rewards file (.trew).
	 * 
	 * This will also export the PRISM MDP model file -- for debugging purposes.
	 * 
	 * @return Pointer to the output explicit model files.
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws IOException
	 */
	public PrismExplicitModelPointer exportExplicitModelFiles() throws XMDPException, PrismException, IOException {
		// Get MDP translation with QAs as the reward structures -- so that we can export the reward files
		String mdpStr = mMDPTranslator.getMDPTranslation(true);

		// Create explicit model pointer to output directory **for models**
		// PrismRewardTranslator only uses transition rewards
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(
				mSettings.getModelOutputPath(), DEFAULT_MODEL_FILENAME_PREFIX, PrismRewardType.TRANSITION_REWARD);

		// Export .sta, .tra, .lab, and .trew files
		mPrismAPI.exportExplicitModelFiles(mdpStr, outputExplicitModelPointer);

		// Export .mdp file for debugging purposes
		mPrismAPI.exportMDPModelFile(mdpStr, outputExplicitModelPointer);

		return outputExplicitModelPointer;
	}

	/**
	 * Generate an optimal policy (the objective is the cost function) of the MDP. Compute its QA values. Cache its
	 * expected total cost and QA values.
	 * 
	 * This method is only applicable to the total-cost criterion.
	 * 
	 * @return An optimal policy, if exists.
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 */
	public PolicyInfo generateOptimalPolicy()
			throws XMDPException, PrismException, ResultParsingException, IOException {
		legalCostCriterionCheck(CostCriterion.TOTAL_COST);

		String mdp = mMDPTranslator.getMDPTranslation(false);

		// Goal with cost-minimizing objective
		String goalProperty = mMDPTranslator.getGoalPropertyTranslation(mCostCriterion);

		// Compute an optimal policy, and cache its total cost and QA values
		return computeOptimalPolicy(mdp, goalProperty, mSettings.getAdversaryOutputPath());
	}

	/**
	 * Generate an optimal policy w.r.t. a given objective function, that satisfies a given constraint. Compute its QA
	 * values and its expected total cost (not the objective value). Cache its expected total cost and QA values.
	 * 
	 * This method is only applicable to the total-cost criteria.
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
	public PolicyInfo generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction<?, ?>> constraint)
			throws XMDPException, PrismException, ResultParsingException, IOException {
		legalCostCriterionCheck(CostCriterion.TOTAL_COST);

		PrismRewardTranslator rewardTranslator = mMDPTranslator.getPrismRewardTranslator();
		PrismPropertyTranslator propTranslator = mMDPTranslator.getPrismPropertyTransltor();

		StringBuilder mdpBuilder = new StringBuilder();
		String originalMDPStr = mMDPTranslator.getMDPTranslation(false);
		mdpBuilder.append(originalMDPStr);

		// Include the QA function of the value to be constrained
		String constrainedQARewards = rewardTranslator.getQAFunctionTranslation(constraint.getQFunction());
		mdpBuilder.append("\n\n");
		mdpBuilder.append(constrainedQARewards);

		// Include the objective function
		String objectiveRewards = rewardTranslator.getObjectiveFunctionTranslation(objectiveFunction);
		mdpBuilder.append("\n\n");
		mdpBuilder.append(objectiveRewards);

		String mdpStr = mdpBuilder.toString();
		String propertyStr = propTranslator.buildMDPConstrainedMinProperty(mXMDP.getGoal(), objectiveFunction,
				constraint);

		// Compute an optimal policy that satisfies the constraint, and cache its total cost and QA values
		String advOutputPath = mSettings.getAdversaryOutputPath() + "_" + constraint.getQFunction().getName();
		return computeOptimalPolicy(mdpStr, propertyStr, advOutputPath);
	}

	/**
	 * Helper method to compute an optimal policy. Cache the policy's expected total cost and QA values.
	 * 
	 * @param mdpStr
	 *            : MDP string with reward structure(s)
	 * @param propertyStr
	 *            : Property string for either minimizing the cost function or other objective function
	 * @param advOutputPath
	 *            : Output path for PRISM explicit model files, including adversary (.adv) file
	 * @return An optimal policy, if exists.
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 * @throws XMDPException
	 */
	private PolicyInfo computeOptimalPolicy(String mdpStr, String propertyStr, String advOutputPath)
			throws PrismException, ResultParsingException, IOException, XMDPException {
		// Create explicit model pointer to output directory **for adversary**
		// PrismRewardTranslator only uses transition rewards
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(advOutputPath,
				DEFAULT_MODEL_FILENAME_PREFIX, PrismRewardType.TRANSITION_REWARD);

		// Create explicit model reader of the output model
		PrismExplicitModelReader explicitModelReader = new PrismExplicitModelReader(outputExplicitModelPointer,
				mMDPTranslator.getValueEncodingScheme());

		if (isMultiObjectiveProperty(propertyStr)) {
			// Configure PRISM for multi-objective strategy synthesis
			File prodStaOutputFile = outputExplicitModelPointer.getProductStatesFile();
			mPrismAPI.configureForMultiObjectiveStrategySynthesis(prodStaOutputFile);
		}

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
			mCachedCosts.put(policy, result);
		} else {
			// The objective function in the property is not the cost function
			// Calculate the expected total cost of the policy, and cache it
			computeAndCacheCost(policy);
		}

		// Compute and cache all of the QA values of the policy
		computeAndCacheAllQAValues(policy);

		return buildPolicyInfo(policy);
	}

	private void legalCostCriterionCheck(CostCriterion costCriterion) {
		if (mCostCriterion != costCriterion) {
			throw new UnsupportedOperationException();
		}
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
	 * 
	 * @param propertyStr
	 * @return Whether the property is multi-objective
	 */
	private boolean isMultiObjectiveProperty(String propertyStr) {
		return propertyStr.startsWith("multi");
	}

	public PolicyInfo buildPolicyInfo(Policy policy) throws ResultParsingException, XMDPException, PrismException {
		double objectiveCost = computeObjectiveCost(policy);
		PolicyInfo policyInfo = new PolicyInfo(mXMDP, policy, objectiveCost);

		CostFunction costFunction = mXMDP.getCostFunction();

		for (IQFunction<?, ?> qFunction : mXMDP.getQSpace()) {
			// QA value
			double qaValue = computeQAValue(policy, qFunction);
			policyInfo.putQAValue(qFunction, qaValue);

			// Scaled QA cost
			AttributeCostFunction<?> attrCostFunction = costFunction.getAttributeCostFunction(qFunction);
			double nonScaledQACost = computeQACost(policy, qFunction);
			double scaledQACost = nonScaledQACost * costFunction.getScalingConstant(attrCostFunction);
			policyInfo.putScaledQACost(qFunction, scaledQACost);

			if (qFunction instanceof NonStandardMetricQFunction<?, ?, ?>) {
				// Event-based QA value
				NonStandardMetricQFunction<?, ?, IEvent<?, ?>> nonStdQFunction = (NonStandardMetricQFunction<?, ?, IEvent<?, ?>>) qFunction;
				EventBasedQAValue<IEvent<?, ?>> eventBasedQAValue = computeEventBasedQAValue(policy, nonStdQFunction);
				policyInfo.putEventBasedQAValue(nonStdQFunction, eventBasedQAValue);
			}
		}
		return policyInfo;
	}

	/**
	 * Retrieve the objective cost (depending on the cost criterion of the MDP) of a given policy from the cache. If the
	 * policy is not already in the cache, then compute and cache its objective cost.
	 * 
	 * @param policy
	 *            : Policy
	 * @return Objective cost of the policy (depending on the cost criterion of the MDP)
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double computeObjectiveCost(Policy policy) throws XMDPException, PrismException, ResultParsingException {
		if (!mCachedCosts.containsKey(policy)) {
			computeAndCacheCost(policy);
		}
		return mCachedCosts.get(policy);
	}

	private void computeAndCacheCost(Policy policy) throws XMDPException, PrismException, ResultParsingException {
		XDTMC xdtmc = new XDTMC(mXMDP, policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc);
		String dtmc = dtmcTranslator.getDTMCTranslation(false, false);
		String queryProperty = dtmcTranslator.getCostQueryPropertyTranslation(mCostCriterion);
		double totalCost = mPrismAPI.queryPropertyFromDTMC(dtmc, queryProperty);
		mCachedCosts.put(policy, totalCost);
	}

	/**
	 * Retrieve the QA value of a given policy from the cache. If the policy is not already in the cache, then compute
	 * and cache all of its QA values.
	 * 
	 * @param policy
	 *            : Policy
	 * @param qFunction
	 *            : QA function
	 * @return QA value of the policy
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double computeQAValue(Policy policy, IQFunction<?, ?> qFunction)
			throws XMDPException, PrismException, ResultParsingException {
		if (!mXMDP.getQSpace().contains(qFunction)) {
			throw new QFunctionNotFoundException(qFunction);
		}
		if (!mCachedQAValues.containsKey(policy)) {
			computeAndCacheAllQAValues(policy);
		}
		return mCachedQAValues.get(policy).get(qFunction);
	}

	private void computeAndCacheAllQAValues(Policy policy)
			throws XMDPException, PrismException, ResultParsingException {
		XDTMC xdtmc = new XDTMC(mXMDP, policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true, false);

		Map<IQFunction<?, ?>, String> queryProperties = new HashMap<>();
		for (IQFunction<?, ?> qFunction : mXMDP.getQSpace()) {
			String queryProperty = dtmcTranslator.getNumQueryPropertyTranslation(qFunction, mCostCriterion);
			queryProperties.put(qFunction, queryProperty);
		}

		// Compute and cache the QA values of the policy
		Map<IQFunction<?, ?>, Double> qaValues = computeValues(dtmcWithQAs, queryProperties);
		mCachedQAValues.put(policy, qaValues);
	}

	/**
	 * Retrieve the QA cost of a given policy from the cache. If the policy is not already in the cache, then compute
	 * and cache all of its QA costs.
	 * 
	 * @param policy
	 *            : Policy
	 * @param qFunction
	 *            : QA function
	 * @return QA cost of the policy
	 * @throws ResultParsingException
	 * @throws XMDPException
	 * @throws PrismException
	 */
	public double computeQACost(Policy policy, IQFunction<?, ?> qFunction)
			throws ResultParsingException, XMDPException, PrismException {
		if (!mCachedQACosts.containsKey(policy)) {
			computeAndCacheAllQACosts(policy);
		}
		return mCachedQACosts.get(policy).get(qFunction);
	}

	private void computeAndCacheAllQACosts(Policy policy) throws XMDPException, ResultParsingException, PrismException {
		XDTMC xdtmc = new XDTMC(mXMDP, policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc);
		String dtmcWithQACosts = dtmcTranslator.getDTMCTranslation(false, true);

		Map<IQFunction<?, ?>, String> queryProperties = new HashMap<>();
		for (IQFunction<?, ?> qFunction : mXMDP.getQSpace()) {
			AttributeCostFunction<?> attrCostFunction = mXMDP.getCostFunction().getAttributeCostFunction(qFunction);
			String queryProperty = dtmcTranslator.getQACostQueryPropertyTranslation(attrCostFunction, mCostCriterion);
			queryProperties.put(qFunction, queryProperty);
		}

		// Compute and cache the QA costs of the policy
		Map<IQFunction<?, ?>, Double> qaCosts = computeValues(dtmcWithQACosts, queryProperties);
		mCachedQACosts.put(policy, qaCosts);
	}

	private <E> Map<E, Double> computeValues(String dtmcModelStr, Map<E, String> queryProperties)
			throws ResultParsingException, PrismException {
		// Build a string containing all properties to be computed (1 property/line)
		StringBuilder builder = new StringBuilder();
		for (String queryProperty : queryProperties.values()) {
			builder.append(queryProperty);
			builder.append("\n");
		}
		String propertiesStr = builder.toString();

		// Get result of each property
		Map<String, Double> results = mPrismAPI.queryPropertiesFromDTMC(dtmcModelStr, propertiesStr);

		// Pair results to the functions that compute them
		Map<E, Double> objValues = new HashMap<>();
		for (Entry<E, String> entry : queryProperties.entrySet()) {
			E obj = entry.getKey();
			String queryProperty = entry.getValue();
			objValues.put(obj, results.get(queryProperty));
		}
		return objValues;
	}

	public void computeQAValuesFromExplicitDTMC(PrismExplicitModelPointer explicitDTMCPointer,
			Iterable<IQFunction<IAction, ITransitionStructure<IAction>>> qFunctions)
			throws XMDPException, PrismException, ResultParsingException {
		PrismPropertyTranslator propertyTranslator = mMDPTranslator.getPrismPropertyTransltor();
		ValueEncodingScheme encodings = mMDPTranslator.getValueEncodingScheme();
		String rawRewardQuery = propertyTranslator.buildDTMCRawRewardQueryProperty(mXMDP.getGoal(), mCostCriterion);

		// Cache the QA values of the policy
		Map<IQFunction<?, ?>, Double> qaValues = new HashMap<>();
		for (IQFunction<?, ?> qFunction : qFunctions) {
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
	 *            : Policy
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
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc);
		String dtmc = dtmcTranslator.getDTMCTranslation(false, false);
		String eventCounterRewards = dtmcTranslator.getEventCounterRewardsTranslation(qFunction.getEventBasedMetric());

		StringBuilder dtmcStrBuilder = new StringBuilder();
		dtmcStrBuilder.append(dtmc);
		dtmcStrBuilder.append("\n\n");
		dtmcStrBuilder.append(eventCounterRewards);
		String dtmcWithEventCounters = dtmcStrBuilder.toString();

		Map<E, String> eventQueryProps = new HashMap<>();
		StringBuilder propsStrBuilder = new StringBuilder();
		for (E event : qFunction.getEventBasedMetric().getEvents()) {
			String eventQueryProp = dtmcTranslator.getEventCountPropertyTranslation(event, mCostCriterion);
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
