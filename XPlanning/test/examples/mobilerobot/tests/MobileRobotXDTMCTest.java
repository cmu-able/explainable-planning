package examples.mobilerobot.tests;

import java.io.File;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import language.dtmc.XDTMC;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.policy.Policy;
import prism.PrismException;
import prismconnector.PrismAPIWrapper;
import prismconnector.PrismConfiguration;
import prismconnector.PrismExplicitModelPointer;
import prismconnector.PrismExplicitModelReader;
import prismconnector.PrismMDPTranslator;
import prismconnector.PrismRewardType;
import prismconnector.exceptions.ResultParsingException;

public class MobileRobotXDTMCTest {

	@Test(dataProvider = "xdtmcSolutions")
	public void testXDTMCConstructor(PrismExplicitModelPointer explicitDTMCPointer, XDTMC xdtmc) {
		printExplicitModelDirName(explicitDTMCPointer);
	}

	@DataProvider(name = "xdtmcSolutions")
	public Object[][] xdtmcSolutions() {
		File prismDir = new File(MobileRobotXMDPTest.PRISM_OUTPUT_PATH);
		File[] explicitModelDirs = prismDir.listFiles();
		for (File explicitModelDir : explicitModelDirs) {
			PrismExplicitModelPointer explicitModelPointer = new PrismExplicitModelPointer(
					explicitModelDir.getAbsolutePath());
		}

		// TODO
		return new Object[][] { new Object[] { 1, "a" }, new Object[] { 2, "b" }, };
	}

	private void generateAdversaries() throws IOException, ParseException, MapTopologyException, ResultParsingException,
			XMDPException, PrismException {
		String mapJsonDirPath = MobileRobotXMDPTest.MAPS_PATH;
		String missionJsonDirPath = MobileRobotXMDPTest.MISSIONS_PATH;

		MobileRobotTestLoader testLoader = new MobileRobotTestLoader(mapJsonDirPath, missionJsonDirPath);
		File missionJsonDir = new File(missionJsonDirPath);
		File[] missionJsonFiles = missionJsonDir.listFiles();

		int i = 0;
		for (File missionJsonFile : missionJsonFiles) {
			XMDP xmdp = testLoader.loadXMDP(missionJsonFile);
			Policy policy = generateAdverary(missionJsonFile, xmdp);
			// TODO
		}
	}

	private Policy generateAdverary(File missionJsonFile, XMDP xmdp)
			throws XMDPException, PrismException, ResultParsingException, IOException {
		String outputPath = MobileRobotXMDPTest.PRISM_OUTPUT_PATH + "/" + missionJsonFile.getName();
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath, "adv");

		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true, PrismRewardType.STATE_REWARD);
		String mdpWithQAs = mdpTranslator.getMDPTranslation(true);
		String goalProperty = mdpTranslator.getGoalPropertyTranslation();

		// Default PRISM configuration
		PrismConfiguration prismConfig = new PrismConfiguration();

		PrismAPIWrapper prismAPI = new PrismAPIWrapper(prismConfig);
		prismAPI.generateMDPAdversary(mdpWithQAs, goalProperty, outputExplicitModelPointer);

		PrismExplicitModelReader explicitModelReader = new PrismExplicitModelReader(
				mdpTranslator.getValueEncodingScheme(), outputExplicitModelPointer);
		return explicitModelReader.readPolicyFromFiles();
	}

	private void printExplicitModelDirName(PrismExplicitModelPointer explicitDTMCPointer) {
		System.out.println("Adversary: " + explicitDTMCPointer.getExplicitModelDirectory().getName());
	}
}
