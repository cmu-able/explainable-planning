package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import explanation.analysis.AlternativeExplorer;
import language.dtmc.XDTMC;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.metrics.IQFunction;
import language.policy.Policy;
import prism.PrismException;
import solver.prismconnector.PrismConfiguration;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.PrismDTMCTranslator;
import solver.prismconnector.PrismRewardType;
import solver.prismconnector.exceptions.ResultParsingException;

public class MobileRobotAlternativeExplorerTest {

	@Test(dataProvider = "xmdpProblems")
	public void testGenerateTravelTimeAlternative(File missionJsonFile, PrismConnector prismConnector, Policy policy) {
		AlternativeExplorer altExplorer = new AlternativeExplorer(prismConnector, policy);
		IQFunction<?, ?> qFunction = prismConnector.getXMDP().getQSpace().getQFunction(TravelTimeQFunction.class,
				TravelTimeQFunction.NAME);

		try {
			Policy altPolicy = altExplorer.getParetoOptimalImmediateNeighbor(qFunction);

			if (altPolicy == null) {
				SimpleConsoleLogger
						.log(String.format("No alternative found that improves %s .", TravelTimeQFunction.NAME));
			} else {
				printPrismDTMCAndProperties(prismConnector, altPolicy);
			}
			SimpleConsoleLogger.newLine();
		} catch (ResultParsingException | XMDPException | PrismException | IOException e) {
			e.printStackTrace();
			fail("Exception thrown while findign an alternative");
		}
	}

	@Test(dataProvider = "xmdpProblems")
	public void testAlternativeExplorer(File missionJsonFile, PrismConnector prismConnector, Policy policy) {
		AlternativeExplorer altExplorer = new AlternativeExplorer(prismConnector, policy);

		try {
			Set<Policy> altPolicies = altExplorer.getParetoOptimalImmediateNeighbors();
			String message;
			if (altPolicies.isEmpty()) {
				message = "There is no Pareto-optimal alternative.";
			} else if (altPolicies.size() == 1) {
				message = String.format("There is %d alternative.", altPolicies.size());
			} else {
				message = String.format("There are %d alternatives.", altPolicies.size());
			}
			SimpleConsoleLogger.log(message);
			SimpleConsoleLogger.newLine();

			for (Policy altPolicy : altPolicies) {
				printPrismDTMCAndProperties(prismConnector, altPolicy);
				SimpleConsoleLogger.newLine();
			}
		} catch (ResultParsingException | XMDPException | PrismException | IOException e) {
			e.printStackTrace();
			fail("Exception thrown while finding alternative policies");
		}
	}

	private void printPrismDTMCAndProperties(PrismConnector prismConnector, Policy policy)
			throws XMDPException, ResultParsingException, PrismException {
		XDTMC xdtmc = new XDTMC(prismConnector.getXMDP(), policy);
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.TRANSITION_REWARD);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);

		SimpleConsoleLogger.log("Transition-reward DTMC Translation (with QAs)", dtmcWithQAs, false);
		SimpleConsoleLogger.newLine();

		for (IQFunction<?, ?> qFunction : xdtmc.getXMDP().getQSpace()) {
			double qaValue = prismConnector.getQAValue(policy, qFunction);

			SimpleConsoleLogger.log(String.format("QA value of %s", qFunction.getName()), qaValue, true);
		}
	}

	@BeforeMethod
	public void printMissionFilename(Object[] data) throws ResultParsingException, XMDPException, PrismException {
		File missionJsonFile = (File) data[0];
		PrismConnector prismConnector = (PrismConnector) data[1];
		Policy policy = (Policy) data[2];

		SimpleConsoleLogger.log("Mission", missionJsonFile.getName(), true);
		SimpleConsoleLogger.logHeader("Solution Policy");
		printPrismDTMCAndProperties(prismConnector, policy);
		SimpleConsoleLogger.newLine();
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] loadXMDPs() throws IOException, ParseException, MapTopologyException, PrismException,
			ResultParsingException, XMDPException {
		String mapJsonDirPath = MobileRobotXMDPTest.MAPS_PATH;
		String missionJsonDirPath = MobileRobotXMDPTest.MISSIONS_PATH;

		MobileRobotTestLoader testLoader = new MobileRobotTestLoader(mapJsonDirPath, missionJsonDirPath);
		File missionJsonDir = new File(missionJsonDirPath);
		File[] missionJsonFiles = missionJsonDir.listFiles();
		Object[][] data = new Object[missionJsonFiles.length][3];

		int i = 0;
		for (File missionJsonFile : missionJsonFiles) {
			// XMDP problem
			XMDP xmdp = testLoader.loadXMDP(missionJsonFile);

			// PrismConnector
			String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
			String outputPath = MobileRobotXMDPTest.PRISM_OUTPUT_PATH + "/" + missionName;
			PrismConfiguration prismConfig = new PrismConfiguration();
			PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(false, outputPath, prismConfig);
			PrismConnector prismConn = new PrismConnector(xmdp, prismConnSetttings);

			// Optimal policy
			Policy policy = prismConn.generateOptimalPolicy();

			// Pass mission.json, PRISM connector, and policy as data
			data[i] = new Object[] { missionJsonFile, prismConn, policy };
			i++;
		}
		return data;
	}

	@AfterClass
	private void terminatePrismConnector(Object[] data) {
		PrismConnector prismConnector = (PrismConnector) data[1];
		// Close down PRISM
		prismConnector.terminate();
	}
}
