package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import language.dtmc.XDTMC;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.policy.Policy;
import prism.PrismException;
import prismconnector.PrismAPIWrapper;
import prismconnector.PrismConfiguration;
import prismconnector.PrismDTMCTranslator;
import prismconnector.PrismExplicitModelPointer;
import prismconnector.PrismExplicitModelReader;
import prismconnector.PrismMDPTranslator;
import prismconnector.PrismRewardType;
import prismconnector.exceptions.ResultParsingException;

public class MobileRobotXDTMCTest {

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismDTMCTranslatorStateReward(PrismExplicitModelPointer explicitDTMCPointer, XDTMC xdtmc) {
		TravelTimeQFunction timeQFunction = xdtmc.getXMDP().getQSpace().getQFunction(TravelTimeQFunction.class);

		try {
			PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
			String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);
			String timeQueryTranslation = dtmcTranslator.getNumQueryPropertyTranslation(timeQFunction);
			System.out.println("State-reward DTMC Translation (with QAs):");
			System.out.println(dtmcWithQAs);
			System.out.println();
			System.out.println("Time Query Property Translation:");
			System.out.println(timeQueryTranslation);
			System.out.println();
		} catch (XMDPException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XDTMC to PRISM DTMC");
		}
	}

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismDTMCTranslatorTransitionReward(PrismExplicitModelPointer explicitDTMCPointer, XDTMC xdtmc) {
		TravelTimeQFunction timeQFunction = xdtmc.getXMDP().getQSpace().getQFunction(TravelTimeQFunction.class);

		try {
			PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true,
					PrismRewardType.TRANSITION_REWARD);
			String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);
			String timeQueryTranslation = dtmcTranslator.getNumQueryPropertyTranslation(timeQFunction);
			System.out.println("Transition-reward DTMC Translation (with QAs):");
			System.out.println(dtmcWithQAs);
			System.out.println();
			System.out.println("Time Query Property Translation:");
			System.out.println(timeQueryTranslation);
			System.out.println();
		} catch (XMDPException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XDTMC to PRISM DTMC");
		}
	}

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismExplicitDTMCPropertyQuery(PrismExplicitModelPointer explicitDTMCPointer, XDTMC xdtmc) {
		String propertyStr = "R=? [ F rLoc=0 & readyToCopy ]";

		// Default PRISM configuration
		PrismConfiguration prismConfig = new PrismConfiguration();

		try {
			PrismAPIWrapper prismAPI = new PrismAPIWrapper(prismConfig);
			double totalCost = prismAPI.queryPropertyFromExplicitDTMC(propertyStr, explicitDTMCPointer, 1);
			double totalTime = prismAPI.queryPropertyFromExplicitDTMC(propertyStr, explicitDTMCPointer, 2);
			System.out.println("Query values from explicit DTMC file...");
			System.out.print("Query property: ");
			System.out.println(propertyStr);
			System.out.print("Expected total cost of adversary: ");
			System.out.println(totalCost);
			System.out.print("Expected total time of adversary: ");
			System.out.println(totalTime);
			System.out.println();
		} catch (PrismException | ResultParsingException e) {
			e.printStackTrace();
			fail("Exception thrown while PRISM model checking DTCM property");
		}
	}

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismDTMCPropertyQuery(PrismExplicitModelPointer explicitDTMCPointer, XDTMC xdtmc)
			throws XMDPException {
		TravelTimeQFunction timeQFunction = xdtmc.getXMDP().getQSpace().getQFunction(TravelTimeQFunction.class);

		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);
		String timeQuery = dtmcTranslator.getNumQueryPropertyTranslation(timeQFunction);

		// Default PRISM configuration
		PrismConfiguration prismConfig = new PrismConfiguration();

		try {
			PrismAPIWrapper prismAPI = new PrismAPIWrapper(prismConfig);
			double result = prismAPI.queryPropertyFromDTMC(dtmcWithQAs, timeQuery);
			System.out.println("Query value from DTMC...");
			System.out.print("Query property: ");
			System.out.println(timeQuery);
			System.out.print("Expected total time: ");
			System.out.println(result);
			System.out.println();
		} catch (PrismException | ResultParsingException e) {
			e.printStackTrace();
			fail("Exception thrown while PRISM model checking DTCM property");
		}
	}

	@DataProvider(name = "xdtmcSolutions")
	public Object[][] generateAdversaries() throws IOException, ParseException, MapTopologyException,
			ResultParsingException, XMDPException, PrismException {
		String mapJsonDirPath = MobileRobotXMDPTest.MAPS_PATH;
		String missionJsonDirPath = MobileRobotXMDPTest.MISSIONS_PATH;

		MobileRobotTestLoader testLoader = new MobileRobotTestLoader(mapJsonDirPath, missionJsonDirPath);
		File missionJsonDir = new File(missionJsonDirPath);
		File[] missionJsonFiles = missionJsonDir.listFiles();
		Object[][] data = new Object[missionJsonFiles.length][2];

		int i = 0;
		for (File missionJsonFile : missionJsonFiles) {
			XMDP xmdp = testLoader.loadXMDP(missionJsonFile);
			PrismExplicitModelReader explicitDTMCReader = generateAdverary(missionJsonFile, xmdp);
			Policy policy = explicitDTMCReader.readPolicyFromFiles();
			XDTMC xdtmc = new XDTMC(xmdp, policy);
			PrismExplicitModelPointer explicitDTMCPointer = explicitDTMCReader.getPrismExplicitModelPointer();
			data[i] = new Object[] { explicitDTMCPointer, xdtmc };
			i++;
		}
		return data;
	}

	private PrismExplicitModelReader generateAdverary(File missionJsonFile, XMDP xmdp)
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

		PrismExplicitModelReader explicitDTMCReader = new PrismExplicitModelReader(
				mdpTranslator.getValueEncodingScheme(), outputExplicitModelPointer);
		return explicitDTMCReader;
	}

	@BeforeMethod
	public void printExplicitModelDirName(Object[] data) {
		PrismExplicitModelPointer explicitDTMCPointer = (PrismExplicitModelPointer) data[0];
		System.out.println("Adversary: " + explicitDTMCPointer.getExplicitModelDirectory().getName());
	}
}
