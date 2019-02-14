package examples.clinicscheduling.tests;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.models.IAction;
import language.exceptions.QFunctionNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.objectives.AttributeCostFunction;
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
import solver.gurobiconnector.GRBSolverUtils;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.QFunctionEncodingScheme;
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
		AverageCostMDPSolver solver = new AverageCostMDPSolver(explicitMDP, null, hardConstraints,
				GRBSolverUtils.DEFAULT_FEASIBILITY_TOL, GRBSolverUtils.DEFAULT_INT_FEAS_TOL);
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
		// Objective cost function for ExplicitMDP (not necessarily the same as the cost function of XMDP) is always
		// at index 0
		int numCostFunctions = explicitMDP.getNumCostFunctions();
		double[] occupancyCosts = new double[numCostFunctions]; // Cost and QA values
		double[] occupancyQACosts = new double[numCostFunctions]; // (non-scaled) QA costs

		// From GRBSolver:
		// Compute occupancy QA values and costs of the optimal policy
		computeOccupancyCosts(xResults, explicitMDP, occupancyCosts, occupancyQACosts);

		// From PRISM:
		GRBPolicyReader policyReader = new GRBPolicyReader(mPrismExplicitModelReader);
		Policy policy = policyReader.readPolicyFromPolicyMatrix(policyMatrix, explicitMDP);

		// Compute the average cost and QA values of the optimal policy
		PolicyInfo policyInfo = mPrismConnector.buildPolicyInfo(policy);

		// Use the cost functions and the QA functions from the XMDP
		CostFunction costFunction = mPrismConnector.getXMDP().getCostFunction();
		QSpace qFunctions = mPrismConnector.getXMDP().getQSpace();

		double[][] diffs = compare(occupancyCosts, occupancyQACosts, policyInfo, costFunction, qFunctions);
		// Excluding the element at index 0 since we don't compute its value
		double[][] tailDiffs = Arrays.copyOfRange(diffs, 1, diffs.length);
		printSummaryStatistics(tailDiffs);
		return occupancyCosts;
	}

	private void printSummaryStatistics(double[][] diffs) {
		DescriptiveStatistics diffStats = new DescriptiveStatistics();
		DescriptiveStatistics percentDiffStats = new DescriptiveStatistics();

		for (int k = 0; k < diffs.length; k++) {
			double diff = diffs[k][0];
			double percentDiff = diffs[k][1];
			double absDiff = Math.abs(diff);
			double absPercentDiff = Math.abs(percentDiff);
			diffStats.addValue(absDiff);
			percentDiffStats.addValue(absPercentDiff);
		}

		double minDiff = diffStats.getMin();
		double maxDiff = diffStats.getMax();
		double meanDiff = diffStats.getMean();
		double stdDiff = diffStats.getStandardDeviation();

		double minPercentDiff = percentDiffStats.getMin();
		double maxPercentDiff = percentDiffStats.getMax();
		double meanPercentDiff = percentDiffStats.getMean();
		double stdPercentDiff = percentDiffStats.getStandardDeviation();

		if (maxDiff > 0) {
			System.out.println("Differences in policy values computed from LP and MC:");
			System.out.println("minDiff: " + minDiff + ", minPercentDiff: " + minPercentDiff);
			System.out.println("maxDiff: " + maxDiff + ", maxPercentDiff: " + maxPercentDiff);
			System.out.println("meanDiff: " + meanDiff + ", meanPercentDiff: " + meanPercentDiff);
			System.out.println("stdDiff: " + stdDiff + ", stdPercentDiff: " + stdPercentDiff);
		}
	}

	private double[][] compare(double[] occupancyCosts, double[] occupancyQACosts, PolicyInfo policyInfo,
			CostFunction costFunction, QSpace qFunctions) throws QFunctionNotFoundException {
		double[][] diffs = new double[occupancyCosts.length][3];

		ValueEncodingScheme encodings = mPrismExplicitModelReader.getValueEncodingScheme();

		int objCostIndex = encodings.getRewardStructureIndex(costFunction);
		double occupancyObjCost = occupancyCosts[objCostIndex];
		double objCost = policyInfo.getObjectiveCost();
		// assertEquals(objCost, occupancyObjCost, mEqualityTol, "Objective costs are not equal");
		double objCostDiff = occupancyObjCost - objCost;

		if (Math.abs(objCostDiff) > mEqualityTol) {
			double percentObjCostDiff = objCost != 0 ? objCostDiff / objCost : Double.POSITIVE_INFINITY;
			diffs[objCostIndex][0] = objCostDiff;
			diffs[objCostIndex][1] = percentObjCostDiff;
		}

		for (IQFunction<IAction, ITransitionStructure<IAction>> qFunction : qFunctions) {
			int k = encodings.getRewardStructureIndex(qFunction);

			// From LP method:
			double occupancyQAValue = occupancyCosts[k];
			double nonScaledOccupancyQACost = occupancyQACosts[k];
			double scalingConst = costFunction.getScalingConstant(costFunction.getAttributeCostFunction(qFunction));
			double scaledOccupancyQACost = scalingConst * nonScaledOccupancyQACost;

			// From MC method:
			double qaValue = policyInfo.getQAValue(qFunction);
			double scaledQACost = policyInfo.getScaledQACost(qFunction);

			// assertEquals(qaValue, occupancyCost, mEqualityTol, qFunction.getName() + " values are not equal");
			double qaValueDiff = occupancyQAValue - qaValue;
			double scaledQACostDiff = scaledOccupancyQACost - scaledQACost;

			if (Math.abs(qaValueDiff) > mEqualityTol) {
				double percentQAValueDiff = qaValue != 0 ? qaValueDiff / qaValue : Double.POSITIVE_INFINITY;
				diffs[k][0] = qaValueDiff;
				diffs[k][1] = percentQAValueDiff;
			}

			if (Math.abs(scaledQACostDiff) > mEqualityTol) {
				diffs[k][2] = scaledQACostDiff;
			}
		}

		return diffs;
	}

	private void computeOccupancyCosts(double[][] xResults, ExplicitMDP explicitMDP, double[] outputOccupancyCosts,
			double[] outputOccupancyQACosts) throws GRBException {
		CostFunction costFunction = mPrismConnector.getXMDP().getCostFunction();
		QFunctionEncodingScheme qFunctionEncoding = mPrismExplicitModelReader.getValueEncodingScheme()
				.getQFunctionEncodingScheme();

		for (int k = QFunctionEncodingScheme.START_REW_STRUCT_INDEX; k < explicitMDP.getNumCostFunctions(); k++) {
			// Cost or QA value
			double occupancyCost = ExplicitModelChecker.computeOccupancyCost(xResults, k, explicitMDP);
			outputOccupancyCosts[k] = occupancyCost;

			if (k >= QFunctionEncodingScheme.START_QA_REW_STRUCT_INDEX) {
				IQFunction<?, ?> qFunction = qFunctionEncoding.getQFunctionAtRewardStructureIndex(k);
				AttributeCostFunction<?> attrCostFunction = costFunction.getAttributeCostFunction(qFunction);
				double costShift = attrCostFunction.getIntercept();
				double costMultiplier = attrCostFunction.getSlope();

				// QA cost
				double occupancyQACost = ExplicitModelChecker.computeOccupancyCost(xResults, k, costShift,
						costMultiplier, explicitMDP);
				outputOccupancyQACosts[k] = occupancyQACost;
			}
		}
	}
}
