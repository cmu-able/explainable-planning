package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.common.DSMException;
import examples.common.Directories;
import examples.mobilerobot.demo.MobileRobotXMDPLoader;
import examples.utils.XMDPDataProvider;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.prismconnector.PrismAPIWrapper;
import solver.prismconnector.PrismMDPTranslator;
import solver.prismconnector.PrismRewardType;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;

public class MobileRobotXMDPTest {

	static final String MAPS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/mobilerobot/maps";
	static final String MISSIONS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/mobilerobot/missions";

	@Test(dataProvider = "xmdpProblems")
	public void testPrismMDPTranslatorTransitionReward(File missionJsonFile, XMDP xmdp) {
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp);

		try {
			String mdpWithQAs = mdpTranslator.getMDPTranslation(true);
			String goalProperty = mdpTranslator.getGoalPropertyTranslation(CostCriterion.TOTAL_COST);
			SimpleConsoleLogger.log("Transition-reward MDP Translation with (with QAs)", mdpWithQAs, false);
			SimpleConsoleLogger.newLine();
			SimpleConsoleLogger.log("Goal Property Translation", goalProperty, false);
			SimpleConsoleLogger.newLine();
		} catch (XMDPException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XMDP to PRISM MDP");
		}
	}

	@Test(dataProvider = "xmdpProblems")
	public void testPrismMDPAdversaryGeneration(File missionJsonFile, XMDP xmdp) throws XMDPException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		String outputPath = Directories.PRISM_ADVS_OUTPUT_PATH + "/" + missionName;
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath, "model",
				PrismRewardType.TRANSITION_REWARD);

		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp);
		String mdpWithQAs = mdpTranslator.getMDPTranslation(true);
		String goalProperty = mdpTranslator.getGoalPropertyTranslation(CostCriterion.TOTAL_COST);

		try {
			PrismAPIWrapper prismAPI = new PrismAPIWrapper();
			SimpleConsoleLogger.log("Generate adverary from transition-reward MDP...");
			double totalCost = prismAPI.generateMDPAdversary(mdpWithQAs, goalProperty, outputExplicitModelPointer);
			SimpleConsoleLogger.log("Expected total cost of adversary", totalCost, true);
			SimpleConsoleLogger.newLine();

			// Close down PRISM
			prismAPI.terminatePrism();
		} catch (FileNotFoundException | PrismException | ResultParsingException e) {
			e.printStackTrace();
			fail("Exception thrown while PRISM generating MDP adversary");
		}
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] loadXMDPs() throws XMDPException, DSMException {
		String mapJsonDirPath = MAPS_PATH;
		String missionJsonDirPath = MISSIONS_PATH;

		SimpleConsoleLogger.log("Loading maps from", mapJsonDirPath, true);
		SimpleConsoleLogger.log("Loading missions from", missionJsonDirPath, true);

		MobileRobotXMDPLoader testLoader = new MobileRobotXMDPLoader(mapJsonDirPath, missionJsonDirPath);
		return XMDPDataProvider.loadXMDPs(missionJsonDirPath, testLoader);
	}

	@BeforeMethod
	public void printMissionFilename(Object[] data) {
		File missionJsonFile = (File) data[0];
		SimpleConsoleLogger.log("Mission", missionJsonFile.getName(), true);
	}

}
