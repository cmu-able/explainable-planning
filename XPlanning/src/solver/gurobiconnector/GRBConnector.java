package solver.gurobiconnector;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeCostFunction;
import language.objectives.CostCriterion;
import language.objectives.CostFunction;
import language.objectives.IAdditiveCostFunction;
import language.policy.Policy;
import solver.common.ExplicitMDP;
import solver.common.ExplicitModelChecker;
import solver.common.LPSolution;
import solver.common.NonStrictConstraint;
import solver.prismconnector.QFunctionEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.explicitmodel.ExplicitMDPReader;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class GRBConnector {

	private XMDP mXMDP;
	private CostCriterion mCostCriterion;
	private GRBConnectorSettings mSettings;
	private QFunctionEncodingScheme mQFunctionEncoding;
	private ExplicitMDPReader mExplicitMDPReader;
	private GRBPolicyReader mPolicyReader;

	// Explicit MDP corresponding to the given XMDP but with empty slots for objective costs (unused)
	private ExplicitMDP mExplicitMDP;

	// Keep track of LP solution corresponding to each policy computed by GRBSolver
	private Map<Policy, LPSolution> mPolicyToLPSolution = new HashMap<>();

	public GRBConnector(XMDP xmdp, CostCriterion costCriterion, GRBConnectorSettings settings)
			throws IOException, ExplicitModelParsingException {
		mXMDP = xmdp;
		mCostCriterion = costCriterion;
		mSettings = settings;

		PrismExplicitModelReader prismExplicitModelReader = settings.getPrismExplicitModelReader();
		mQFunctionEncoding = prismExplicitModelReader.getValueEncodingScheme().getQFunctionEncodingScheme();
		mExplicitMDPReader = new ExplicitMDPReader(prismExplicitModelReader, costCriterion);
		mPolicyReader = new GRBPolicyReader(prismExplicitModelReader);

		// Explicit MDP corresponding to the given XMDP but with empty slots for objective costs (unused)
		mExplicitMDP = mExplicitMDPReader.readExplicitMDP();
	}

	/**
	 * Generate an optimal policy for this unconstrained MDP.
	 * 
	 * @return Optimal policy
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 * @throws XMDPException
	 * @throws GRBException
	 */
	public PolicyInfo generateOptimalPolicy()
			throws IOException, ExplicitModelParsingException, XMDPException, GRBException {
		// Create a new ExplicitMDP for every new objective function, because this method will fill in the
		// ExplicitMDP with the objective costs
		ExplicitMDP explicitMDP = mExplicitMDPReader.readExplicitMDP(mXMDP.getCostFunction());

		// Compute optimal policy, without any cost constraint
		return generateOptimalPolicy(explicitMDP, null, null);
	}

	/**
	 * Generate an optimal policy for this MDP with the given objective function and hard constraint.
	 * 
	 * @param objectiveFunction
	 *            : Optimization objective function
	 * @param attrHardConstraint
	 *            : Hard constraint on a single-attribute cost function
	 * @return Optimal policy
	 * @throws XMDPException
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 * @throws GRBException
	 */
	public PolicyInfo generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction<?, ?>> attrHardConstraint)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException {
		Set<AttributeConstraint<IQFunction<?, ?>>> attrHardConstraints = new HashSet<>();
		attrHardConstraints.add(attrHardConstraint);
		return generateOptimalPolicy(objectiveFunction, attrHardConstraints);
	}

	/**
	 * Generate an optimal policy for this MDP with the given objective function and soft constraint (with hard
	 * constraint).
	 * 
	 * @param objectiveFunction
	 *            : Optimization objective function
	 * @param attrSoftConstraint
	 *            : Soft constraint on a single-attribute cost function
	 * @param attrHardConstraint
	 *            : Hard constraint on a single-attribute cost function
	 * @return Optimal policy
	 * @throws XMDPException
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 * @throws GRBException
	 */
	public PolicyInfo generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction<?, ?>> attrSoftConstraint,
			AttributeConstraint<IQFunction<?, ?>> attrHardConstraint)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException {
		Set<AttributeConstraint<IQFunction<?, ?>>> attrSoftConstraints = new HashSet<>();
		Set<AttributeConstraint<IQFunction<?, ?>>> attrHardConstraints = new HashSet<>();
		attrSoftConstraints.add(attrSoftConstraint);
		attrHardConstraints.add(attrHardConstraint);
		return generateOptimalPolicy(objectiveFunction, attrSoftConstraints, attrHardConstraints);
	}

	public PolicyInfo generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrHardConstraints)
			throws IOException, ExplicitModelParsingException, XMDPException, GRBException {
		// Create a new ExplicitMDP for every new objective function, because this method will fill in the
		// ExplicitMDP with the objective costs
		ExplicitMDP explicitMDP = mExplicitMDPReader.readExplicitMDP(objectiveFunction);

		// Explicit hard (upper or lower) bounds
		NonStrictConstraint[] indexedHardConstraints = CostConstraintUtils
				.createIndexedNonStrictConstraints(attrHardConstraints, mQFunctionEncoding);

		// Compute optimal policy, with the cost constraints
		return generateOptimalPolicy(explicitMDP, null, indexedHardConstraints);
	}

	public PolicyInfo generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrSoftConstraints,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrHardConstraints)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException {
		// Create a new ExplicitMDP for every new objective function, because this method will fill in the
		// ExplicitMDP with the objective costs
		ExplicitMDP explicitMDP = mExplicitMDPReader.readExplicitMDP(objectiveFunction);

		// Explicit soft (upper or lower) bounds
		NonStrictConstraint[] indexedSoftConstraints = CostConstraintUtils
				.createIndexedNonStrictConstraints(attrSoftConstraints, mQFunctionEncoding);

		// Explicit hard (upper or lower) bounds
		NonStrictConstraint[] indexedHardConstraints = CostConstraintUtils
				.createIndexedNonStrictConstraints(attrHardConstraints, mQFunctionEncoding);

		// Compute optimal policy, with the cost constraints
		return generateOptimalPolicy(explicitMDP, indexedSoftConstraints, indexedHardConstraints);
	}

	private PolicyInfo generateOptimalPolicy(ExplicitMDP explicitMDP, NonStrictConstraint[] softConstraints,
			NonStrictConstraint[] hardConstraints) throws GRBException, XMDPException, IOException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double[][] policyMatrix = new double[n][m];
		LPSolution solution = null;

		if (mCostCriterion == CostCriterion.TOTAL_COST) {
			SSPSolver solver = new SSPSolver(explicitMDP, softConstraints, hardConstraints, mSettings);
			solution = solver.solveOptimalPolicy(policyMatrix);
		} else if (mCostCriterion == CostCriterion.AVERAGE_COST) {
			AverageCostMDPSolver solver = new AverageCostMDPSolver(explicitMDP, softConstraints, hardConstraints,
					mSettings);
			solution = solver.solveOptimalPolicy(policyMatrix);
		}

		if (solution != null && solution.exists()) {
			Policy policy = mPolicyReader.readPolicyFromPolicyMatrix(policyMatrix, explicitMDP);
			// Keep track of LP solution corresponding to each policy computed by GRBSolver
			mPolicyToLPSolution.put(policy, solution);
			return buildPolicyInfo(policy);
		}

		return null;
	}

	public PolicyInfo buildPolicyInfo(Policy policy) throws QFunctionNotFoundException {
		double objectiveCost = computeCost(policy);
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

			// TODO: compute event-based QA values
		}
		return policyInfo;
	}

	public double computeCost(Policy policy) {
		int costFuncIndex = mQFunctionEncoding.getRewardStructureIndex(mXMDP.getCostFunction());
		return computeOccupancyCost(policy, costFuncIndex, 0, 1);
	}

	public double computeQAValue(Policy policy, IQFunction<?, ?> qFunction) throws QFunctionNotFoundException {
		int costFuncIndex = mQFunctionEncoding.getRewardStructureIndex(qFunction);
		return computeOccupancyCost(policy, costFuncIndex, 0, 1);
	}

	public double computeQACost(Policy policy, IQFunction<?, ?> qFunction) throws QFunctionNotFoundException {
		int costFuncIndex = mQFunctionEncoding.getRewardStructureIndex(qFunction);
		AttributeCostFunction<?> attrCostFunction = mXMDP.getCostFunction().getAttributeCostFunction(qFunction);
		double costShift = attrCostFunction.getIntercept();
		double costMultiplier = attrCostFunction.getSlope();
		return computeOccupancyCost(policy, costFuncIndex, costShift, costMultiplier);
	}

	private double computeOccupancyCost(Policy policy, int costFuncIndex, double costShift, double costMultiplier) {
		LPSolution solution = mPolicyToLPSolution.get(policy);
		double[][] xResults = solution.getSolution("x");
		return ExplicitModelChecker.computeOccupancyCost(xResults, costFuncIndex, costShift, costMultiplier,
				mExplicitMDP);
	}
}
