package examples.clinicscheduling.tests;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.objectives.CostCriterion;
import language.objectives.CostFunction;
import language.objectives.IAdditiveCostFunction;
import language.policy.Policy;
import prism.PrismException;
import solver.common.ExplicitMDP;
import solver.common.ExplicitModelChecker;
import solver.common.LPSolution;
import solver.common.NonStrictConstraint;
import solver.gurobiconnector.AverageCostMDPSolver;
import solver.gurobiconnector.GRBPolicyReader;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.ExplicitMDPReader;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class LPMCComparator {

	private PrismConnector mPrismConnector;
	private PrismExplicitModelReader mPrismExplicitModelReader;
	private double mEqualityTol;

	public LPMCComparator(PrismConnector prismConnector, double equalityTol)
			throws XMDPException, PrismException, IOException {
		mPrismConnector = prismConnector;
		mEqualityTol = equalityTol;

		// Export XMDP to explicit model files
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();

		// Create PRISM explicit model reader
		mPrismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr, encodings);
	}

	public ExplicitMDP readExplicitMDP(IAdditiveCostFunction objectiveFunction)
			throws QFunctionNotFoundException, IOException, ExplicitModelParsingException {
		ExplicitMDPReader explicitMDPReader = new ExplicitMDPReader(mPrismExplicitModelReader,
				CostCriterion.AVERAGE_COST);
		ExplicitMDP explicitMDP = explicitMDPReader.readExplicitMDP(objectiveFunction);
		return explicitMDP;
	}

	public double[][] computeOccupationMeasureResults(ExplicitMDP explicitMDP, NonStrictConstraint[] hardConstraints,
			double[][] outputPolicyMatrix) throws GRBException {
		AverageCostMDPSolver solver = new AverageCostMDPSolver(explicitMDP, null, hardConstraints);
		LPSolution solution = solver.solveOptimalPolicy(outputPolicyMatrix);
		double[][] xResults = solution.getSolution("x");
		return xResults;
	}

	/**
	 * Compare the policy's average cost and QA values computed by GRBSolver (LP method), to those computed by PRISM (MC
	 * method). The policy itself is computed by GRBSolver.
	 * 
	 * @param explicitMDP
	 *            : Explicit MDP
	 * @param xResults
	 *            : x results from GRBSolver
	 * @param policyMatrix
	 *            : Policy matrix corresponding to x results
	 * @return Occupancy costs
	 * @throws GRBException
	 * @throws IOException
	 * @throws ResultParsingException
	 * @throws XMDPException
	 * @throws PrismException
	 */
	public double[] checkLPMCConsistency(ExplicitMDP explicitMDP, double[][] xResults, double[][] policyMatrix)
			throws GRBException, IOException, ResultParsingException, XMDPException, PrismException {
		// From GRBSolver:
		// Compute occupation costs of the optimal policy
		double[] occupancyCosts = computeOccupancyCosts(xResults, explicitMDP);

		// From PRISM:
		GRBPolicyReader policyReader = new GRBPolicyReader(mPrismExplicitModelReader);
		Policy policy = policyReader.readPolicyFromPolicyMatrix(policyMatrix, explicitMDP);

		// Use the cost functions and the QA functions from the XMDP
		CostFunction costFunction = mPrismConnector.getXMDP().getCostFunction();
		QSpace qFunctions = mPrismConnector.getXMDP().getQSpace();

		// Compute the average cost and QA values of the optimal policy
		PolicyInfo policyInfo = computePolicyInfo(policy, qFunctions);

		double[] diffs = compare(occupancyCosts, policyInfo, costFunction, qFunctions);
		// Excluding the element at index 0 since we don't compute its value
		double[] tailDiffs = Arrays.copyOfRange(diffs, 1, diffs.length);
		printSummaryStatistics(tailDiffs);
		return occupancyCosts;
	}

	private void printSummaryStatistics(double[] diffs) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (double diff : diffs) {
			double absDiff = Math.abs(diff);
			stats.addValue(absDiff);
		}
		double min = stats.getMin();
		double max = stats.getMax();
		double mean = stats.getMean();
		double std = stats.getStandardDeviation();

		if (max > 0) {
			System.out.println("Differences in policy values computed from LP and MC:");
			System.out.println("min: " + min);
			System.out.println("max: " + max);
			System.out.println("mean: " + mean);
			System.out.println("std: " + std);
		}
	}

	private double[] compare(double[] occupancyCosts, PolicyInfo policyInfo, CostFunction costFunction,
			QSpace qFunctions) throws QFunctionNotFoundException {
		double[] diffs = new double[occupancyCosts.length];

		ValueEncodingScheme encodings = mPrismExplicitModelReader.getValueEncodingScheme();

		int objCostIndex = encodings.getRewardStructureIndex(costFunction);
		double occupancyObjCost = occupancyCosts[objCostIndex];
		double objCost = policyInfo.getObjectiveCost();
		// assertEquals(objCost, occupancyObjCost, mEqualityTol, "Objective costs are not equal");

		if (Math.abs(occupancyObjCost - objCost) > mEqualityTol) {
			diffs[objCostIndex] = occupancyObjCost - objCost;
		}

		for (IQFunction<?, ?> qFunction : policyInfo.getQSpace()) {
			int k = encodings.getRewardStructureIndex(qFunction);
			double occupancyCost = occupancyCosts[k];
			double qaValue = policyInfo.getQAValue(qFunction);
			// assertEquals(qaValue, occupancyCost, mEqualityTol, qFunction.getName() + " values are not equal");

			if (Math.abs(occupancyCost - qaValue) > mEqualityTol) {
				diffs[k] = occupancyCost - qaValue;
			}
		}

		return diffs;
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
