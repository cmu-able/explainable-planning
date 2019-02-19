package examples.clinicscheduling.tests;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.clinicscheduling.demo.ClinicSchedulingDemo;
import examples.clinicscheduling.demo.ClinicSchedulingXMDPLoader;
import examples.common.DSMException;
import examples.common.Directories;
import examples.utils.SimpleConsoleLogger;
import examples.utils.XMDPDataProvider;
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
import solver.gurobiconnector.GRBSolverUtils;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class ClinicSchedulingTest {
	private static final double EQUALITY_TOL = 1e-6;

	@Test(dataProvider = "xmdpProblems")
	public void testUnconstrainedAverageCost(File problemFile, XMDP xmdp) throws PrismException, XMDPException,
			IOException, ExplicitModelParsingException, GRBException, ResultParsingException {
		// Problem file
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		String modelOutputPath = Directories.PRISM_MODELS_OUTPUT_PATH + "/" + problemName;
		String advOutputPath = Directories.PRISM_ADVS_OUTPUT_PATH + "/" + problemName;

		// Create PRISM connector
		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.AVERAGE_COST, prismConnSetttings);

		// Export XMDP to explicit model files
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();

		// Create PRISM explicit model reader
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// Create GRB connector
		GRBConnectorSettings grbConnSettings = new GRBConnectorSettings(prismExplicitModelReader,
				GRBSolverUtils.DEFAULT_FEASIBILITY_TOL, GRBSolverUtils.DEFAULT_INT_FEAS_TOL);
		GRBConnector grbConnector = new GRBConnector(xmdp, CostCriterion.AVERAGE_COST, grbConnSettings);

		LPMCComparator comparator = new LPMCComparator(grbConnector, prismConnector, EQUALITY_TOL);

		// Compute an average-optimal policy using GRBSolver (LP method)
		PolicyInfo solPolicyInfo = grbConnector.generateOptimalPolicy();
		Policy solPolicy = solPolicyInfo.getPolicy();

		// Compare policy evaluation between LP and MC methods
		double[][] diffs = comparator.comparePolicyEvaluation(solPolicy, xmdp.getCostFunction(), xmdp.getQSpace());

		// Print summary statistics
		comparator.printSummaryStatistics(diffs);

		// Close down PRISM
		prismConnector.terminate();
	}

	@Test(dataProvider = "xmdpProblems")
	public void testConstrainedAverageCost(File problemFile, XMDP xmdp) throws PrismException, XMDPException,
			IOException, ExplicitModelParsingException, GRBException, ResultParsingException {
		CostFunction costFunction = xmdp.getCostFunction();
		QSpace qFunctions = xmdp.getQSpace();

		// Problem file
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		String modelOutputPath = Directories.PRISM_MODELS_OUTPUT_PATH + "/" + problemName;
		String advOutputPath = Directories.PRISM_ADVS_OUTPUT_PATH + "/" + problemName;

		// Create PRISM connector
		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.AVERAGE_COST, prismConnSetttings);

		// Export XMDP to explicit model files
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();

		// Create PRISM explicit model reader
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// Create GRB connector
		GRBConnectorSettings grbConnSettings = new GRBConnectorSettings(prismExplicitModelReader,
				GRBSolverUtils.DEFAULT_FEASIBILITY_TOL, GRBSolverUtils.DEFAULT_INT_FEAS_TOL);
		GRBConnector grbConnector = new GRBConnector(xmdp, CostCriterion.AVERAGE_COST, grbConnSettings);

		LPMCComparator comparator = new LPMCComparator(grbConnector, prismConnector, EQUALITY_TOL);

		// Compute an average-optimal policy using GRBSolver (LP method)
		PolicyInfo solPolicyInfo = grbConnector.generateOptimalPolicy();

		for (IQFunction<?, ?> qFunction : qFunctions) {
			double attrCostFuncSlope = costFunction.getAttributeCostFunction(qFunction).getSlope();

			// Strict, hard upper/lower bound
			BOUND_TYPE hardBoundType = attrCostFuncSlope > 0 ? BOUND_TYPE.STRICT_UPPER_BOUND
					: BOUND_TYPE.STRICT_LOWER_BOUND;
			double hardBoundValue = solPolicyInfo.getQAValue(qFunction);
			AttributeConstraint<IQFunction<?, ?>> attrHardConstraint = new AttributeConstraint<IQFunction<?, ?>>(
					qFunction, hardBoundType, hardBoundValue);

			// Policy satisfying the QA constraint
			PolicyInfo policyInfo = grbConnector.generateOptimalPolicy(costFunction, attrHardConstraint);
			Policy policy = policyInfo.getPolicy();

			// Compare policy evaluation between LP and MC methods
			double[][] diffs = comparator.comparePolicyEvaluation(policy, costFunction, qFunctions);

			// Print summary statistics
			comparator.printSummaryStatistics(diffs);
		}

		// Close down PRISM
		prismConnector.terminate();
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] loadXMDPs() throws XMDPException, DSMException {
		String problemsPath = ClinicSchedulingDemo.PROBLEMS_PATH;
		int branchFactor = ClinicSchedulingDemo.DEFAULT_BRANCH_FACTOR;

		ClinicSchedulingXMDPLoader testLoader = new ClinicSchedulingXMDPLoader(problemsPath, branchFactor);
		return XMDPDataProvider.loadXMDPs(problemsPath, testLoader);
	}

	@BeforeMethod
	public void printProblemFilename(Object[] data) {
		File problemFile = (File) data[0];
		SimpleConsoleLogger.log("Problem", problemFile.getName(), true);
	}
}
