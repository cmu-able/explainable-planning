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
import examples.mobilerobot.tests.SimpleConsoleLogger;
import examples.utils.XMDPDataProvider;
import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import language.policy.Policy;
import prism.PrismException;
import solver.common.ExplicitMDP;
import solver.common.ExplicitModelChecker;
import solver.common.LPSolution;
import solver.gurobiconnector.AverageCostMDPSolver;
import solver.gurobiconnector.GRBConnector;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.ExplicitMDPReader;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class ClinicSchedulingUnconstrainedTest {

	@Test(dataProvider = "xmdpProblems")
	public void testAverageQAValues(File problemFile, XMDP xmdp)
			throws PrismException, XMDPException, IOException, ExplicitModelParsingException, GRBException {
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		String modelOutputPath = Directories.PRISM_MODELS_OUTPUT_PATH + "/" + problemName;
		String advOutputPath = Directories.PRISM_ADVS_OUTPUT_PATH + "/" + problemName;

		// Use PrismConnector to export XMDP to explicit model files
		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.AVERAGE_COST, prismConnSetttings);
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// Close down PRISM -- before explainer creates a new PrismConnector
		// prismConnector.terminate();

		// GRBConnector reads from explicit model files
		GRBConnector grbConnector = new GRBConnector(xmdp, CostCriterion.AVERAGE_COST, prismExplicitModelReader);
		Policy policy = grbConnector.generateOptimalPolicy();

		// TODO
		ExplicitMDPReader explicitMDPReader = new ExplicitMDPReader(prismExplicitModelReader,
				CostCriterion.AVERAGE_COST);
		ExplicitMDP explicitMDP = explicitMDPReader.readExplicitMDP(xmdp.getCostFunction());
	}

	private double[] computeOccupancyCosts(ExplicitMDP explicitMDP) throws GRBException {
		int n = explicitMDP.getNumStates();
		int m = explicitMDP.getNumActions();
		double[][] policy = new double[n][m];
		AverageCostMDPSolver solver = new AverageCostMDPSolver(explicitMDP);
		LPSolution solution = solver.solveOptimalPolicy(policy);
		double[][] xResults = solution.getSolution("x");

		double[] occupancyCosts = new double[explicitMDP.getNumCostFunctions()];

		for (int k = 0; k < explicitMDP.getNumCostFunctions(); k++) {
			double occupancyCost = ExplicitModelChecker.computeOccupancyCost(explicitMDP, k, xResults);
			occupancyCosts[k] = occupancyCost;
		}
		return occupancyCosts;
	}

	private PolicyInfo computePolicyInfo(Policy policy, QSpace qSpace, PrismConnector prismConnector)
			throws ResultParsingException, XMDPException, PrismException {
		double cost = prismConnector.getCost(policy);
		PolicyInfo policyInfo = new PolicyInfo(policy, cost);

		for (IQFunction<?, ?> qFunction : qSpace) {
			// For now, only put QA values into policyInfo
			double qaValue = prismConnector.getQAValue(policy, qFunction);
			policyInfo.putQAValue(qFunction, qaValue);
		}
		return policyInfo;
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
