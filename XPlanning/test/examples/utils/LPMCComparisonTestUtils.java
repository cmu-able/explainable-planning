package examples.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import examples.common.XPlannerOutDirectories;
import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeConstraint.BOUND_TYPE;
import language.objectives.CostCriterion;
import language.objectives.CostFunction;
import language.policy.Policy;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.gurobiconnector.GRBConnectorSettings;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class LPMCComparisonTestUtils {

	public static double[][] compareUnconstrainedMDPCase(File problemFile, XMDP xmdp, CostCriterion costCriterion,
			double equalityTol)
			throws PrismException, XMDPException, IOException, PrismConnectorException, GRBException {
		// Problem file
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		String modelOutputPath = "../../../../data/mobilerobots/policies/" + problemName;
		String advOutputPath = "../../../../data/mobilerobots/policies/" + problemName;

		// Create PRISM connector
		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
		PrismConnector prismConnector = new PrismConnector(xmdp, costCriterion, prismConnSetttings);

		// Export XMDP to explicit model files
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();

		// Create PRISM explicit model reader
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// Create GRB connector
		GRBConnectorSettings grbConnSettings = new GRBConnectorSettings(prismExplicitModelReader);
		GRBConnector grbConnector = new GRBConnector(xmdp, costCriterion, grbConnSettings);

		LPMCComparator comparator = new LPMCComparator(grbConnector, prismConnector, equalityTol);

		// Compute an optimal policy for unconstrained MDP
		PolicyInfo solPolicyInfo = null;

		if (costCriterion == CostCriterion.AVERAGE_COST) {
			solPolicyInfo = grbConnector.generateOptimalPolicy();
		} else if (costCriterion == CostCriterion.TOTAL_COST) {
			solPolicyInfo = prismConnector.generateOptimalPolicy();
		}

		Policy solPolicy = solPolicyInfo.getPolicy();

		// Compare policy evaluation between LP and MC methods
		double[][] diffs = comparator.comparePolicyEvaluation(solPolicy, xmdp.getCostFunction(), xmdp.getQSpace());

		// Print summary statistics
		comparator.printSummaryStatistics(diffs);

		// Close down PRISM
		prismConnector.terminate();

		return diffs;
	}

	public static Map<IQFunction<?, ?>, double[][]> compareConstrainedMDPCase(File problemFile, XMDP xmdp,
			CostCriterion costCriterion, double equalityTol)
			throws PrismException, XMDPException, IOException, PrismConnectorException, GRBException {
		CostFunction costFunction = xmdp.getCostFunction();
		QSpace qFunctions = xmdp.getQSpace();

		// Problem file
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		String modelOutputPath = "../../../../data/mobilerobots/policies/" + problemName;
		String advOutputPath = "../../../../data/mobilerobots/policies/" + problemName;

		// Create PRISM connector
		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
		PrismConnector prismConnector = new PrismConnector(xmdp, costCriterion, prismConnSetttings);

		// Export XMDP to explicit model files
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();

		// Create PRISM explicit model reader
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// Create GRB connector
		GRBConnectorSettings grbConnSettings = new GRBConnectorSettings(prismExplicitModelReader);
		GRBConnector grbConnector = new GRBConnector(xmdp, costCriterion, grbConnSettings);

		LPMCComparator comparator = new LPMCComparator(grbConnector, prismConnector, equalityTol);

		// Compute an optimal policy for unconstrained MDP
		PolicyInfo solPolicyInfo = null;

		if (costCriterion == CostCriterion.AVERAGE_COST) {
			solPolicyInfo = grbConnector.generateOptimalPolicy();
		} else if (costCriterion == CostCriterion.TOTAL_COST) {
			solPolicyInfo = prismConnector.generateOptimalPolicy();
		}

		Map<IQFunction<?, ?>, double[][]> resultDiffs = new HashMap<>();

		for (IQFunction<?, ?> qFunction : qFunctions) {
			double attrCostFuncSlope = costFunction.getAttributeCostFunction(qFunction).getSlope();

			// Strict, hard upper/lower bound
			BOUND_TYPE hardBoundType = attrCostFuncSlope > 0 ? BOUND_TYPE.STRICT_UPPER_BOUND
					: BOUND_TYPE.STRICT_LOWER_BOUND;
			double hardBoundValue = solPolicyInfo.getQAValue(qFunction);
			AttributeConstraint<IQFunction<?, ?>> attrHardConstraint = new AttributeConstraint<IQFunction<?, ?>>(
					qFunction, hardBoundType, hardBoundValue);

			// Compute a constraint-satisfying optimal policy -- only GRBSolver supports this
			PolicyInfo policyInfo = grbConnector.generateOptimalPolicy(costFunction, attrHardConstraint);
			Policy policy = policyInfo.getPolicy();

			// Compare policy evaluation between LP and MC methods
			double[][] diffs = comparator.comparePolicyEvaluation(policy, costFunction, qFunctions);

			// Print summary statistics
			comparator.printSummaryStatistics(diffs);
		}

		// Close down PRISM
		prismConnector.terminate();

		return resultDiffs;
	}

	public static boolean allSmallQAValueDifferences(double[][] diffs, double threshold) {
		return allSmallValueDifferences(diffs, threshold, 0);
	}

	public static boolean allSmallPercentQAValueDifferences(double[][] diffs, double threshold) {
		return allSmallValueDifferences(diffs, threshold, 1);
	}

	public static boolean allSmallScaledQACostDifferences(double[][] diffs, double threshold) {
		return allSmallValueDifferences(diffs, threshold, 2);
	}

	private static boolean allSmallValueDifferences(double[][] diffs, double threshold, int valueType) {
		// Excluding the element at index 0 since we don't compute its value
		for (int k = 1; k < diffs.length; k++) {
			double valueDiff = diffs[k][valueType];
			if (valueDiff > threshold) {
				return false;
			}
		}
		return true;
	}
}
