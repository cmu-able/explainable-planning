package solver.gurobiconnector;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import gurobi.GRBException;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.metrics.IQFunction;
import language.objectives.AttributeConstraint;
import language.objectives.CostCriterion;
import language.objectives.IAdditiveCostFunction;
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
		mExplicitMDPReader = new ExplicitMDPReader(prismExplicitModelReader);
		mPolicyReader = new GRBPolicyReader(prismExplicitModelReader);
	}

	public Policy generateOptimalPolicy() throws IOException, ExplicitModelParsingException, QFunctionNotFoundException,
			VarNotFoundException, GRBException {
		// Create a new ExplicitMDP for every new objective function, because this method will fill in the
		// ExplicitMDP with the objective costs
		ExplicitMDP explicitMDP = mExplicitMDPReader.readExplicitMDP(mXMDP.getCostFunction());

		// Compute optimal policy, without any cost constraint
		return generateOptimalPolicy(explicitMDP, null);
	}

	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			AttributeConstraint<IQFunction<?, ?>> attrConstraint)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException {
		Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints = new HashSet<>();
		attrConstraints.add(attrConstraint);
		return generateOptimalPolicy(objectiveFunction, attrConstraints);
	}

	public Policy generateOptimalPolicy(IAdditiveCostFunction objectiveFunction,
			Set<AttributeConstraint<IQFunction<?, ?>>> attrConstraints)
			throws IOException, ExplicitModelParsingException, XMDPException, GRBException {
		// Create a new ExplicitMDP for every new objective function, because this method will fill in the
		// ExplicitMDP with the objective costs
		ExplicitMDP explicitMDP = mExplicitMDPReader.readExplicitMDP(objectiveFunction);

		// Create upper-bounds for costs of ExplicitMDP
		Double[] upperBounds = GRBSolverUtils.createUpperBounds(attrConstraints, mQFunctionEncoding);

		// Compute optimal policy, with the cost constraints
		return generateOptimalPolicy(explicitMDP, upperBounds);
	}

	private Policy generateOptimalPolicy(ExplicitMDP explicitMDP, Double[] upperBounds)
			throws GRBException, VarNotFoundException, IOException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double[][] policy = new double[n][m];
		boolean solutionFound = false;

		if (mCostCriterion == CostCriterion.TOTAL_COST) {
			SSPSolver solver = new SSPSolver(explicitMDP, upperBounds);
			solutionFound = solver.solveOptimalPolicy(policy);
		} else if (mCostCriterion == CostCriterion.AVERAGE_COST) {
			AverageCostMDPSolver solver = new AverageCostMDPSolver(explicitMDP, upperBounds);
			solutionFound = solver.solveOptimalPolicy(policy);
		}

		if (solutionFound) {
			return mPolicyReader.readPolicyFromPolicyMatrix(policy, explicitMDP);
		}

		return null;
	}
}
