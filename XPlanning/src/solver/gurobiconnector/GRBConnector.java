package solver.gurobiconnector;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeConstraint.BOUND_TYPE;
import language.objectives.CostCriterion;
import language.objectives.IAdditiveCostFunction;
import language.objectives.IPenaltyFunction;
import language.policy.Policy;
import solver.common.ExplicitMDP;
import solver.prismconnector.QFunctionEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.explicitmodel.ExplicitMDPReader;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class GRBConnector {

	private XMDP mXMDP;
	private CostCriterion mCostCriterion;
	private QFunctionEncodingScheme mQFunctionEncoding;
	private ExplicitMDPReader mExplicitMDPReader;
	private GRBPolicyReader mPolicyReader;

	public GRBConnector(XMDP xmdp, CostCriterion costCriterion, PrismExplicitModelReader prismExplicitModelReader) {
		mXMDP = xmdp;
		mCostCriterion = costCriterion;
		mQFunctionEncoding = prismExplicitModelReader.getValueEncodingScheme().getQFunctionEncodingScheme();
		mExplicitMDPReader = new ExplicitMDPReader(prismExplicitModelReader, costCriterion);
		mPolicyReader = new GRBPolicyReader(prismExplicitModelReader);
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
	public Policy generateOptimalPolicy()
			throws IOException, ExplicitModelParsingException, XMDPException, GRBException {
		// Create a new ExplicitMDP for every new objective function, because this method will fill in the
		// ExplicitMDP with the objective costs
		ExplicitMDP explicitMDP = mExplicitMDPReader.readExplicitMDP(mXMDP.getCostFunction());

		// Compute optimal policy, without any cost constraint
		return generateOptimalPolicy(explicitMDP, null, null, null, null);
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
	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
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
	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction<?, ?>> attrSoftConstraint,
			AttributeConstraint<IQFunction<?, ?>> attrHardConstraint)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException {
		Set<AttributeConstraint<IQFunction<?, ?>>> attrSoftConstraints = new HashSet<>();
		Set<AttributeConstraint<IQFunction<?, ?>>> attrHardConstraints = new HashSet<>();
		attrSoftConstraints.add(attrSoftConstraint);
		attrHardConstraints.add(attrHardConstraint);
		return generateOptimalPolicy(objectiveFunction, attrSoftConstraints, attrHardConstraints);
	}

	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrHardConstraints)
			throws IOException, ExplicitModelParsingException, XMDPException, GRBException {
		// Create a new ExplicitMDP for every new objective function, because this method will fill in the
		// ExplicitMDP with the objective costs
		ExplicitMDP explicitMDP = mExplicitMDPReader.readExplicitMDP(objectiveFunction);

		// Constraints are on the cost functions starting from index 1 in ExplicitMDP
		// Align the indices of the constraints to those of the cost functions in ExplicitMDP
		int arrayLength = mQFunctionEncoding.getNumRewardStructures() + 1;

		// Explicit hard (upper or lower) bounds
		BOUND_TYPE[] boundTypes = new BOUND_TYPE[arrayLength]; // non-strict bounds
		Double[] hardBoundValues = new Double[arrayLength];

		// Create hard (upper or lower) bounds on costs of ExplicitMDP
		CostConstraintUtils.createExplicitHardBounds(attrHardConstraints, mQFunctionEncoding, boundTypes,
				hardBoundValues);

		// Compute optimal policy, with the cost constraints
		return generateOptimalPolicy(explicitMDP, boundTypes, null, null, hardBoundValues);
	}

	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrSoftConstraints,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrHardConstraints)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException {
		// Create a new ExplicitMDP for every new objective function, because this method will fill in the
		// ExplicitMDP with the objective costs
		ExplicitMDP explicitMDP = mExplicitMDPReader.readExplicitMDP(objectiveFunction);

		// Constraints are on the cost functions starting from index 1 in ExplicitMDP
		// Align the indices of the constraints to those of the cost functions in ExplicitMDP
		int arrayLength = mQFunctionEncoding.getNumRewardStructures() + 1;

		// Explicit soft (upper or lower) bounds
		BOUND_TYPE[] boundTypes = new BOUND_TYPE[arrayLength]; // non-strict bounds
		Double[] softBoundValues = new Double[arrayLength];
		IPenaltyFunction[] penaltyFunctions = new IPenaltyFunction[arrayLength];

		// Explicit hard (upper or lower) bounds
		Double[] hardBoundValues = new Double[arrayLength];

		// Create soft (upper or lower) bounds (and penalty functions) on costs of Explicit MDP
		CostConstraintUtils.createExplicitSoftBounds(attrSoftConstraints, mQFunctionEncoding, boundTypes,
				softBoundValues, penaltyFunctions);

		// Create hard (upper or lower) bounds on costs of ExplicitMDP
		CostConstraintUtils.createExplicitHardBounds(attrHardConstraints, mQFunctionEncoding, boundTypes,
				hardBoundValues);

		// Compute optimal policy, with the cost constraints
		return generateOptimalPolicy(explicitMDP, boundTypes, softBoundValues, penaltyFunctions, hardBoundValues);
	}

	private Policy generateOptimalPolicy(ExplicitMDP explicitMDP, BOUND_TYPE[] boundTypes, Double[] softBoundValues,
			IPenaltyFunction[] penaltyFunctions, Double[] hardBoundValues)
			throws GRBException, VarNotFoundException, IOException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double[][] policy = new double[n][m];
		boolean solutionFound = false;

		if (mCostCriterion == CostCriterion.TOTAL_COST) {
			SSPSolver solver;
			if (softBoundValues != null) {
				solver = new SSPSolver(explicitMDP, softBoundValues, penaltyFunctions, hardBoundValues);
			} else if (hardBoundValues != null) {
				solver = new SSPSolver(explicitMDP, hardBoundValues);
			} else {
				solver = new SSPSolver(explicitMDP);
			}
			solutionFound = solver.solveOptimalPolicy(policy);
		} else if (mCostCriterion == CostCriterion.AVERAGE_COST) {
			AverageCostMDPSolver solver;
			if (softBoundValues != null) {
				solver = new AverageCostMDPSolver(explicitMDP, softBoundValues, penaltyFunctions, hardBoundValues);
			} else if (hardBoundValues != null) {
				solver = new AverageCostMDPSolver(explicitMDP, hardBoundValues);
			} else {
				solver = new AverageCostMDPSolver(explicitMDP);
			}
			solutionFound = solver.solveOptimalPolicy(policy);
		}

		if (solutionFound) {
			return mPolicyReader.readPolicyFromPolicyMatrix(policy, explicitMDP);
		}

		return null;
	}
}
