package examples.mobilerobot.tests;

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.common.DSMException;
import examples.mobilerobot.demo.MobileRobotXPlanner;
import examples.mobilerobot.demo.MobileRobotXMDPLoader;
import examples.utils.LPMCComparisonTestUtils;
import examples.utils.SimpleConsoleLogger;
import examples.utils.XMDPDataProvider;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;

public class MobileRobotLPMCComparisonTest {
	private static final double EQUALITY_TOL = 1e-6;

	@Test(dataProvider = "xmdpProblems")
	public void testUnconstrainedAverageCost(File problemFile, XMDP xmdp)
			throws PrismException, XMDPException, IOException, GRBException, PrismConnectorException {
		double[][] diffs = LPMCComparisonTestUtils.compareUnconstrainedMDPCase(problemFile, xmdp,
				CostCriterion.AVERAGE_COST, EQUALITY_TOL);
		checkDifferences(diffs);
	}

	@Test(dataProvider = "xmdpProblems")
	public void testConstrainedAverageCost(File problemFile, XMDP xmdp)
			throws PrismException, XMDPException, IOException, GRBException, PrismConnectorException {
		Map<IQFunction<?, ?>, double[][]> resultDiffs = LPMCComparisonTestUtils.compareConstrainedMDPCase(problemFile,
				xmdp, CostCriterion.AVERAGE_COST, EQUALITY_TOL);

		for (Entry<IQFunction<?, ?>, double[][]> e : resultDiffs.entrySet()) {
			double[][] diffs = e.getValue();
			checkDifferences(diffs);
		}
	}

	private void checkDifferences(double[][] diffs) {
		assertTrue(LPMCComparisonTestUtils.allSmallQAValueDifferences(diffs, 1), "Large QA value difference");
		assertTrue(LPMCComparisonTestUtils.allSmallPercentQAValueDifferences(diffs, 1),
				"Large percentage QA value difference");
		assertTrue(LPMCComparisonTestUtils.allSmallScaledQACostDifferences(diffs, 1), "Large scaled QA cost difference");
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] loadXMDPs() throws XMDPException, DSMException {
		String mapsJsonDirPath = MobileRobotXPlanner.MAPS_PATH;
		String missionsJsonDirPath = MobileRobotXPlanner.MISSIONS_PATH;
		File mapsJsonDir = new File(mapsJsonDirPath);

		MobileRobotXMDPLoader testLoader = new MobileRobotXMDPLoader(mapsJsonDir);
		return XMDPDataProvider.loadXMDPs(missionsJsonDirPath, testLoader);
	}

	@BeforeMethod
	public void printMissionFilename(Object[] data) {
		File missionJsonFile = (File) data[0];
		SimpleConsoleLogger.log("Mission", missionJsonFile.getName(), true);
	}
}
