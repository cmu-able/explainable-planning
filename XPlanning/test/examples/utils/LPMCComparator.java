package examples.utils;

import java.io.IOException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.objectives.CostFunction;
import language.policy.Policy;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class LPMCComparator {

	private GRBConnector mGRBConnector;
	private PrismConnector mPrismConnector;
	private PrismExplicitModelReader mPrismExplicitModelReader;
	private double mEqualityTol;

	public LPMCComparator(GRBConnector grbConnector, PrismConnector prismConnector, double equalityTol) {
		mGRBConnector = grbConnector;
		mPrismConnector = prismConnector;
		mEqualityTol = equalityTol;
	}

	/**
	 * Compare policy evaluation of the cost function and the QA functions between using LP and MC methods.
	 * 
	 * @param policy
	 *            : Policy to be evaluated
	 * @param costFunction
	 *            : Cost function
	 * @param qFunctions
	 *            : QA functions
	 * @return Differences between policy values computed using LP and MC methods
	 * @throws XMDPException
	 * @throws IOException
	 * @throws ExplicitModelParsingException
	 * @throws GRBException
	 * @throws ResultParsingException
	 * @throws PrismException
	 */
	public double[][] comparePolicyEvaluation(Policy policy, CostFunction costFunction, QSpace qFunctions)
			throws XMDPException, IOException, ExplicitModelParsingException, GRBException, ResultParsingException,
			PrismException {
		PolicyInfo policyInfoGRB = mGRBConnector.buildPolicyInfo(policy);
		PolicyInfo policyInfoPrism = mPrismConnector.buildPolicyInfo(policy);

		int arrayLength = mPrismExplicitModelReader.getValueEncodingScheme().getNumRewardStructures() + 1;
		double[][] diffs = new double[arrayLength][3];

		// From LP method:
		double objCostGRB = policyInfoGRB.getObjectiveCost();

		// From MC method:
		double objCostPrism = policyInfoPrism.getObjectiveCost();

		// assertEquals(objCostPrism, objCostGRB, mEqualityTol, "Objective costs are not equal");

		double objCostDiff = objCostGRB - objCostPrism;

		if (Math.abs(objCostDiff) > mEqualityTol) {
			double percentObjCostDiff = objCostPrism != 0 ? objCostDiff / objCostPrism : Double.POSITIVE_INFINITY;
			int objCostIndex = mPrismExplicitModelReader.getValueEncodingScheme().getRewardStructureIndex(costFunction);
			diffs[objCostIndex][0] = objCostDiff;
			diffs[objCostIndex][1] = percentObjCostDiff;
		}

		for (IQFunction<?, ?> qFunction : qFunctions) {
			// From LP method:
			double rawQAValueGRB = policyInfoGRB.getQAValue(qFunction);
			double scaledQACostGRB = policyInfoGRB.getScaledQACost(qFunction);

			// From MC method:
			double rawQAValuePrism = policyInfoPrism.getQAValue(qFunction);
			double scaledQACostPrism = policyInfoPrism.getScaledQACost(qFunction);

			// assertEquals(rawQAValuePrism, rawQAValueGRB, mEqualityTol, qFunction.getName() + " values are not
			// equal");

			double rawQAValueDiff = rawQAValueGRB - rawQAValuePrism;
			double scaledQACostDiff = scaledQACostGRB - scaledQACostPrism;

			int k = mPrismExplicitModelReader.getValueEncodingScheme().getRewardStructureIndex(qFunction);

			if (Math.abs(rawQAValueDiff) > mEqualityTol) {
				double percentQAValueDiff = rawQAValuePrism != 0 ? rawQAValueDiff / rawQAValuePrism
						: Double.POSITIVE_INFINITY;
				diffs[k][0] = rawQAValueDiff;
				diffs[k][1] = percentQAValueDiff;
			}

			if (Math.abs(scaledQACostDiff) > mEqualityTol) {
				diffs[k][2] = scaledQACostDiff;
			}
		}
		return diffs;
	}

	public void printSummaryStatistics(double[][] diffs) {
		DescriptiveStatistics diffStats = new DescriptiveStatistics();
		DescriptiveStatistics percentDiffStats = new DescriptiveStatistics();

		// Excluding the element at index 0 since we don't compute its value
		for (int k = 1; k < diffs.length; k++) {
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
}
