package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import language.exceptions.IncompatibleActionException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import prism.PrismException;
import prismconnector.PrismAPIWrapper;
import prismconnector.PrismConfiguration;
import prismconnector.PrismExplicitModelPointer;
import prismconnector.PrismMDPTranslator;
import prismconnector.PrismRewardType;
import prismconnector.exceptions.ResultParsingException;

public class MobileRobotXMDPTest {

	static final String MAPS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/maps";
	static final String MISSIONS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/missions";
	static final String PRISM_OUTPUT_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/prism";

	@Test(dataProvider = "xmdpProblems")
	public void testPrismMDPTranslatorStateReward(File missionJsonFile, XMDP xmdp) throws XMDPException {
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true, PrismRewardType.STATE_REWARD);

		try {
			String mdpWithQAs = mdpTranslator.getMDPTranslation(true);
			String goalProperty = mdpTranslator.getGoalPropertyTranslation();
			System.out.println("State-reward MDP Translation (with QAs):");
			System.out.println(mdpWithQAs);
			System.out.println();
			System.out.println("Goal Property Translation:");
			System.out.println(goalProperty);
			System.out.println();
		} catch (XMDPException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XMDP to PRISM MDP");
		}
	}

	@Test(dataProvider = "xmdpProblems")
	public void testPrismMDPTranslatorTransitionReward(File missionJsonFile, XMDP xmdp) {
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true, PrismRewardType.TRANSITION_REWARD);

		try {
			String mdpWithQAs = mdpTranslator.getMDPTranslation(true);
			String goalProperty = mdpTranslator.getGoalPropertyTranslation();
			System.out.println("Transition-reward MDP Translation with (with QAs):");
			System.out.println(mdpWithQAs);
			System.out.println();
			System.out.println("Goal Property Translation:");
			System.out.println(goalProperty);
			System.out.println();
		} catch (XMDPException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XMDP to PRISM MDP");
		}
	}

	@Test(dataProvider = "xmdpProblems")
	public void testPrismMDPAdversaryGeneration(File missionJsonFile, XMDP xmdp) throws XMDPException {
		String outputPath = PRISM_OUTPUT_PATH + "/" + missionJsonFile.getName();
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath, "adv");

		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true, PrismRewardType.STATE_REWARD);
		String mdpWithQAs = mdpTranslator.getMDPTranslation(true);
		String goalProperty = mdpTranslator.getGoalPropertyTranslation();

		// Default PRISM configuration
		PrismConfiguration prismConfig = new PrismConfiguration();

		try {
			PrismAPIWrapper prismAPI = new PrismAPIWrapper(prismConfig);
			double totalCost = prismAPI.generateMDPAdversary(mdpWithQAs, goalProperty, outputExplicitModelPointer);
			System.out.println("Generate adverary from state-reward MDP...");
			System.out.print("Expected total cost of adversary: ");
			System.out.println(totalCost);
			System.out.println();
		} catch (FileNotFoundException | PrismException | ResultParsingException e) {
			e.printStackTrace();
			fail("Exception thrown while PRISM generating MDP adversary");
		}
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] xmdpProblems()
			throws IncompatibleActionException, IOException, ParseException, MapTopologyException {
		String mapJsonDirPath = MAPS_PATH;
		String missionJsonDirPath = MISSIONS_PATH;

		MobileRobotTestLoader testLoader = new MobileRobotTestLoader(mapJsonDirPath, missionJsonDirPath);
		File missionJsonDir = new File(missionJsonDirPath);
		File[] missionJsonFiles = missionJsonDir.listFiles();
		Object[][] data = new Object[missionJsonFiles.length][2];

		int i = 0;
		for (File missionJsonFile : missionJsonFiles) {
			XMDP xmdp = testLoader.loadXMDP(missionJsonFile);
			data[i] = new Object[] { missionJsonFile, xmdp };
		}
		return data;
	}

	@BeforeMethod
	public void printMissionFilename(Object[] data) {
		File missionJsonFile = (File) data[0];
		System.out.println("Mission: " + missionJsonFile.getName());
	}

}
