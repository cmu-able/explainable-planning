package solver.prismconnector;

import java.io.FileNotFoundException;
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
import language.objectives.CostCriterion;
import language.objectives.IAdditiveCostFunction;
import language.policy.Policy;
import language.qfactors.IAction;
import prism.PrismException;
import solver.prismconnector.PrismConfiguration.PrismEngine;
import solver.prismconnector.PrismConfiguration.PrismMDPMultiSolutionMethod;
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
	private Map<PrismExplicitModelPointer, Policy> mExplicitModelPtrToPolicy = new HashMap<>();

	public PrismConnector(XMDP xmdp, CostCriterion costCriterion, PrismConnectorSettings settings)
			throws PrismException {
		mXMDP = xmdp;
		mCostCriterion = costCriterion;
		mMDPTranslator = new PrismMDPTranslator(xmdp);
		mSettings = settings;
		mPrismAPI = new PrismAPIWrapper(settings.getPrismConfiguration());
	}

	public XMDP getXMDP() {
		return mXMDP;
	}

	public PrismMDPTranslator getPrismMDPTranslator() {
		return mMDPTranslator;
	}

	/**
	 * Export the PRISM explicit model files from this XMDP. The explicit model files include: states file (.sta),
	 * transitions file (.tra), labels file (.lab), and transition rewards file (.trew).
	 * 
	 * @return Pointer to the output explicit model files.
	 * @throws XMDPException
	 * @throws FileNotFoundException
	 * @throws PrismException
	 */
	public PrismExplicitModelPointer exportExplicitModelFiles()
			throws XMDPException, FileNotFoundException, PrismException {
		// Get MDP translation with QAs as the reward structures -- so that we can export the reward files
		String mdpStr = mMDPTranslator.getMDPTranslation(true);

		// Create explicit model pointer to output directory **for models**
		// PrismRewardTranslator only uses transition rewards
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(
				mSettings.getModelOutputPath(), DEFAULT_MODEL_FILENAME_PREFIX, PrismRewardType.TRANSITION_REWARD);

		// Export .sta, .tra, .lab, and .trew files
		mPrismAPI.exportExplicitModelFiles(mdpStr, outputExplicitModelPointer);

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
	public Policy generateOptimalPolicy() throws XMDPException, PrismException, ResultParsingException, IOException {
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
	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction<?, ?>> constraint)
			throws XMDPException, PrismException, ResultParsingException, IOException {
		legalCostCriterionCheck(CostCriterion.TOTAL_COST);

		configureForMultiObjectiveStrategySynthesis();

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

	private void legalCostCriterionCheck(CostCriterion costCriterion) {
		if (mCostCriterion != costCriterion) {
			throw new UnsupportedOperationException();
		}
	}

	private void configureForMultiObjectiveStrategySynthesis() {
		// Use transition rewards for multi-objective adversary synthesis
		// PrismRewardTranslator already uses transition rewards

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
	 * @param advOutputPath
	 *            : Output path for PRISM explicit model files, including adversary (.adv) file
	 * @return An optimal policy, if exists.
	 * @throws PrismException
	 * @throws ResultParsingException
	 * @throws IOException
	 * @throws XMDPException
	 */
	private Policy computeOptimalPolicy(String mdpStr, String propertyStr, String advOutputPath)
			throws PrismException, ResultParsingException, IOException, XMDPException {
		// Create explicit model pointer to output directory **for adversary**
		// PrismRewardTranslator only uses transition rewards
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(advOutputPath,
				DEFAULT_MODEL_FILENAME_PREFIX, PrismRewardType.TRANSITION_REWARD);

		// Create explicit model reader of the output model
		PrismExplicitModelReader explicitModelReader = new PrismExplicitModelReader(outputExplicitModelPointer,
				mMDPTranslator.getValueEncodingScheme());

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
			computeCost(policy);
		}

		// Compute and cache the QA values of the policy
		computeQAValues(policy, mXMDP.getQSpace());

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
	 * Retrieve the cost (depending on the cost criterion of the MDP) of a given policy from the cache. If the policy is
	 * not already in the cache, then compute and cache its cost.
	 * 
	 * @param policy
	 * @return Cost of the policy (depending on the cost criterion of the MDP)
	 * @throws XMDPException
	 * @throws PrismException
	 * @throws ResultParsingException
	 */
	public double getCost(Policy policy) throws XMDPException, PrismException, ResultParsingException {
		if (!mCachedCosts.containsKey(policy)) {
			computeCost(policy);
		}
		return mCachedCosts.get(policy);
	}

	private void computeCost(Policy policy) throws XMDPException, PrismException, ResultParsingException {
		XDTMC xdtmc = new XDTMC(mXMDP, policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc);
		String dtmc = dtmcTranslator.getDTMCTranslation(false);
		String queryProperty = dtmcTranslator.getCostQueryPropertyTranslation(mCostCriterion);
		double totalCost = mPrismAPI.queryPropertyFromDTMC(dtmc, queryProperty);
		mCachedCosts.put(policy, totalCost);
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
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);

		Map<IQFunction<?, ?>, String> queryProperties = new HashMap<>();
		StringBuilder builder = new StringBuilder();
		for (IQFunction<?, ?> qFunction : qSpace) {
			String queryProperty = dtmcTranslator.getNumQueryPropertyTranslation(qFunction, mCostCriterion);
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
