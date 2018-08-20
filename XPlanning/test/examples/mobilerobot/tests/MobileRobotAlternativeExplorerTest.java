package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.AlternativeExplorer;
import language.dtmc.XDTMC;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.metrics.IQFunction;
import language.policy.Policy;
import prism.PrismException;
import prismconnector.PrismConfiguration;
import prismconnector.PrismConnector;
import prismconnector.PrismConnectorSettings;
import prismconnector.PrismDTMCTranslator;
import prismconnector.PrismRewardType;
import prismconnector.exceptions.ResultParsingException;

public class MobileRobotAlternativeExplorerTest {

	@Test(dataProvider = "xmdpProblems")
	public void testAlternativeExplorer(File missionJsonFile, PrismConnector prismConnector, Policy policy) {
		AlternativeExplorer altExplorer = new AlternativeExplorer(prismConnector, policy);

		try {
			Set<Policy> altPolicies = altExplorer.getParetoOptimalImmediateNeighbors();
			System.out.println("Alternatives:");
			System.out.println();

			for (Policy altPolicy : altPolicies) {
				printPrismDTMCAndProperties(prismConnector, altPolicy);
				System.out.println();
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
		System.out.println("Transition-reward DTMC Translation (with QAs):");
		System.out.println(dtmcWithQAs);
		System.out.println();

		for (IQFunction<?, ?> qFunction : xdtmc.getXMDP().getQSpace()) {
			double qaValue = prismConnector.getQAValue(policy, qFunction);
			System.out.print("QA value of ");
			System.out.print(qFunction.getName() + ": ");
			System.out.println(qaValue);
		}
	}

	@BeforeMethod
	public void printMissionFilename(Object[] data) throws ResultParsingException, XMDPException, PrismException {
		File missionJsonFile = (File) data[0];
		PrismConnector prismConnector = (PrismConnector) data[1];
		Policy policy = (Policy) data[2];
		System.out.println("Mission: " + missionJsonFile.getName());
		System.out.println("Solution Policy:");
		printPrismDTMCAndProperties(prismConnector, policy);
		System.out.println();
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

			data[i] = new Object[] { missionJsonFile, prismConn, policy };
		}
		return data;
	}
}
