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
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.objectives.AttributeConstraint;
import language.objectives.AttributeConstraint.BOUND_TYPE;
import language.objectives.CostCriterion;
import language.objectives.CostFunction;
import prism.PrismException;
import solver.common.ExplicitMDP;
import solver.common.NonStrictConstraint;
import solver.gurobiconnector.CostConstraintUtils;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;

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

		LPMCComparator comparator = new LPMCComparator(prismConnector, EQUALITY_TOL);

		// Compute an average-optimal policy using GRBSolver (LP method)
		ExplicitMDP explicitMDP = comparator.readExplicitMDP(xmdp.getCostFunction());
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double[][] outputPolicyMatrix = new double[n][m];
		double[][] xResults = comparator.computeOccupationMeasureResults(explicitMDP, null, outputPolicyMatrix);

		// Check result consistency between LP method and MC method
		comparator.checkLPMCConsistency(explicitMDP, xResults, outputPolicyMatrix);

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

		LPMCComparator comparator = new LPMCComparator(prismConnector, EQUALITY_TOL);

		// Compute an average-optimal policy using GRBSolver (LP method)
		ExplicitMDP explicitMDP = comparator.readExplicitMDP(costFunction);
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double[][] outputPolicyMatrix = new double[n][m];
		double[][] xResults = comparator.computeOccupationMeasureResults(explicitMDP, null, outputPolicyMatrix);

		// Check result consistency between LP method and MC method
		double[] occupancyCosts = comparator.checkLPMCConsistency(explicitMDP, xResults, outputPolicyMatrix);

		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();

		for (IQFunction<?, ?> qFunction : qFunctions) {
			double attrCostFuncSlope = costFunction.getAttributeCostFunction(qFunction).getSlope();

			// Strict, hard upper/lower bound
			BOUND_TYPE hardBoundType = attrCostFuncSlope > 0 ? BOUND_TYPE.STRICT_UPPER_BOUND
					: BOUND_TYPE.STRICT_LOWER_BOUND;
			int k = encodings.getRewardStructureIndex(qFunction);
			double hardBoundValue = occupancyCosts[k];
			AttributeConstraint<IQFunction<?, ?>> attrConstraint = new AttributeConstraint<IQFunction<?, ?>>(qFunction,
					hardBoundType, hardBoundValue);

			NonStrictConstraint[] hardConstraints = CostConstraintUtils
					.createIndexedNonStrictConstraints(attrConstraint, encodings.getQFunctionEncodingScheme());

			double[][] constrainedOutputPolicyMatrix = new double[n][m];
			double[][] constrainedxResults = comparator.computeOccupationMeasureResults(explicitMDP, hardConstraints,
					constrainedOutputPolicyMatrix);

			comparator.checkLPMCConsistency(explicitMDP, constrainedxResults, constrainedOutputPolicyMatrix);
		}
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
