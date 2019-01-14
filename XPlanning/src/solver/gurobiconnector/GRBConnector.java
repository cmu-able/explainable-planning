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
import language.objectives.CostCriterion;
import language.objectives.IAdditiveCostFunction;
import language.policy.Policy;
import solver.common.ExplicitMDP;
import solver.common.NonStrictConstraint;
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
		NonStrictConstraint[] indexedHardConstraints = new NonStrictConstraint[arrayLength];
		CostConstraintUtils.fillIndexedNonStrictConstraints(attrHardConstraints, mQFunctionEncoding,
				indexedHardConstraints);

		// Compute optimal policy, with the cost constraints
		return generateOptimalPolicy(explicitMDP, null, indexedHardConstraints);
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
		NonStrictConstraint[] indexedSoftConstraints = new NonStrictConstraint[arrayLength];
		CostConstraintUtils.fillIndexedNonStrictConstraints(attrSoftConstraints, mQFunctionEncoding,
				indexedSoftConstraints);

		// Explicit hard (upper or lower) bounds
		NonStrictConstraint[] indexedHardConstraints = new NonStrictConstraint[arrayLength];
		CostConstraintUtils.fillIndexedNonStrictConstraints(attrHardConstraints, mQFunctionEncoding,
				indexedHardConstraints);

		// Compute optimal policy, with the cost constraints
		return generateOptimalPolicy(explicitMDP, indexedSoftConstraints, indexedHardConstraints);
	}

	private Policy generateOptimalPolicy(ExplicitMDP explicitMDP, NonStrictConstraint[] softConstraints,
			NonStrictConstraint[] hardConstraints) throws GRBException, VarNotFoundException, IOException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double[][] policy = new double[n][m];
		boolean solutionFound = false;

		if (mCostCriterion == CostCriterion.TOTAL_COST) {
			SSPSolver solver = new SSPSolver(explicitMDP, softConstraints, hardConstraints);
			solutionFound = solver.solveOptimalPolicy(policy);
		} else if (mCostCriterion == CostCriterion.AVERAGE_COST) {
			AverageCostMDPSolver solver = new AverageCostMDPSolver(explicitMDP, softConstraints, hardConstraints);
			solutionFound = solver.solveOptimalPolicy(policy);
		}

		if (solutionFound) {
			return mPolicyReader.readPolicyFromPolicyMatrix(policy, explicitMDP);
		}

		return null;
	}
}
