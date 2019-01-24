package examples.clinicscheduling.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.objectives.CostCriterion;
import language.objectives.CostFunction;
import language.policy.Policy;
import prism.PrismException;
import solver.common.ExplicitMDP;
import solver.common.ExplicitModelChecker;
import solver.common.LPSolution;
import solver.gurobiconnector.AverageCostMDPSolver;
import solver.gurobiconnector.GRBPolicyReader;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.ExplicitMDPReader;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class LPMCComparator {

	private PrismConnector mPrismConnector;
	private PrismExplicitModelReader mPrismExplicitModelReader;
	private GRBPolicyReader mPolicyReader;
	private double mEqualityTol;

	public LPMCComparator(PrismConnector prismConnector, PrismExplicitModelReader prismExplicitModelReader,
			double equalityTol) {
		mPrismConnector = prismConnector;
		mPrismExplicitModelReader = prismExplicitModelReader;
		mPolicyReader = new GRBPolicyReader(prismExplicitModelReader);
		mEqualityTol = equalityTol;
	}

	/**
	 * Compare the policy's average cost and QA values computed by GRBSolver (LP method), to those computed by PRISM (MC
	 * method). The policy itself is computed by GRBSolver.
	 * 
	 * @throws XMDPException
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 * @throws GRBException
	 * @throws ResultParsingException
	 * @throws PrismException
	 */
	public void compare() throws XMDPException, IOException, ExplicitModelParsingException, GRBException,
			ResultParsingException, PrismException {
		// Use the cost functions and the QA functions from the XMDP
		CostFunction costFunction = mPrismConnector.getXMDP().getCostFunction();
		QSpace qFunctions = mPrismConnector.getXMDP().getQSpace();

		// From GRBSolver:
		// First, compute an optimal policy
		ExplicitMDPReader explicitMDPReader = new ExplicitMDPReader(mPrismExplicitModelReader,
				CostCriterion.AVERAGE_COST);
		ExplicitMDP explicitMDP = explicitMDPReader.readExplicitMDP(costFunction);
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double[][] outputPolicyMatrix = new double[n][m];
		double[][] xResults = computeOccupationMeasureResults(explicitMDP, outputPolicyMatrix);

		// Compute occupation costs of the optimal policy
		double[] occupancyCosts = computeOccupancyCosts(xResults, explicitMDP);

		// From PRISM:
		Policy policy = mPolicyReader.readPolicyFromPolicyMatrix(outputPolicyMatrix, explicitMDP);

		// Compute the average cost and QA values of the optimal policy
		PolicyInfo policyInfo = computePolicyInfo(policy, qFunctions);

		compare(occupancyCosts, policyInfo, costFunction, qFunctions);
	}

	private void compare(double[] occupancyCosts, PolicyInfo policyInfo, CostFunction costFunction, QSpace qFunctions)
			throws QFunctionNotFoundException {
		ValueEncodingScheme encodings = mPrismConnector.getPrismMDPTranslator().getValueEncodingScheme();

		int objCostIndex = encodings.getRewardStructureIndex(costFunction);
		double occupancyObjCost = occupancyCosts[objCostIndex];
		double objCost = policyInfo.getObjectiveCost();

		assertEquals(objCost, occupancyObjCost, mEqualityTol, "Objective costs are not equal");

		for (IQFunction<?, ?> qFunction : policyInfo.getQSpace()) {
			int k = encodings.getRewardStructureIndex(qFunction);
			double occupancyCost = occupancyCosts[k];
			double qaValue = policyInfo.getQAValue(qFunction);

			assertEquals(qaValue, occupancyCost, mEqualityTol, qFunction.getName() + " values are not equal");
		}
	}

	private double[][] computeOccupationMeasureResults(ExplicitMDP explicitMDP, double[][] outputPolicy)
			throws GRBException {
		AverageCostMDPSolver solver = new AverageCostMDPSolver(explicitMDP);
		LPSolution solution = solver.solveOptimalPolicy(outputPolicy);
		double[][] xResults = solution.getSolution("x");
		return xResults;
	}

	private double[] computeOccupancyCosts(double[][] xResults, ExplicitMDP explicitMDP) throws GRBException {
		int numCostFunctions = explicitMDP.getNumCostFunctions();

		// Objective cost function for ExplicitMDP (not necessarily the same as the cost function of XMDP) is always
		// at index 0
		double[] occupancyCosts = new double[numCostFunctions];

		for (int k = 0; k < explicitMDP.getNumCostFunctions(); k++) {
			double occupancyCost = ExplicitModelChecker.computeOccupancyCost(explicitMDP, k, xResults);
			occupancyCosts[k] = occupancyCost;
		}
		return occupancyCosts;
	}

	private PolicyInfo computePolicyInfo(Policy policy, QSpace qFunctions)
			throws ResultParsingException, XMDPException, PrismException {
		double objectiveCost = mPrismConnector.getCost(policy);
		// For now, policyInfo only has the policy's cost and QA values
		PolicyInfo policyInfo = new PolicyInfo(policy, objectiveCost);

		for (IQFunction<?, ?> qFunction : qFunctions) {
			// For now, only put QA values into policyInfo
			double qaValue = mPrismConnector.getQAValue(policy, qFunction);
			policyInfo.putQAValue(qFunction, qaValue);
		}
		return policyInfo;
	}
}
