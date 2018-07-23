package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;

import dtmc.XDTMC;
import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.ResultParsingException;
import exceptions.VarNotFoundException;
import exceptions.XMDPException;
import mdp.XMDP;
import prism.PrismException;
import prismconnector.PrismAPIWrapper;
import prismconnector.PrismConfiguration;
import prismconnector.PrismDTMCTranslator;
import prismconnector.PrismExplicitModelPointer;
import prismconnector.PrismMDPTranslator;
import prismconnector.PrismRewardType;

class MobileRobotTest {

	private MobileRobotTestProblem mTestProblem;

	MobileRobotTest() {
		mTestProblem = new MobileRobotTestProblem();
	}

	@Test
	public void testMDPConstructor() {
		try {
			XMDP xmdp = mTestProblem.createXMDP();
		} catch (XMDPException e) {
			e.printStackTrace();
			fail("Exception thrown while creating XMDP");
		}
	}

	@Test
	public void testPrismMDPTranslatorStateReward() throws XMDPException {
		XMDP xmdp = mTestProblem.createXMDP();
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

	@Test
	public void testPrismMDPTranslatorTransitionReward() throws XMDPException {
		XMDP xmdp = mTestProblem.createXMDP();
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

	@Test
	public void testDTMCConstructor() {
		try {
			XDTMC xdtmc = mTestProblem.createXDTMC();
		} catch (XMDPException e) {
			e.printStackTrace();
			fail("Exception thrown while creating XDTMC");
		}
	}

	@Test
	public void testPrismDTMCTranslatorStateReward() throws XMDPException {
		XDTMC xdtmc = mTestProblem.createXDTMC();

		try {
			PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
			String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);
			String timeQueryTranslation = dtmcTranslator.getNumQueryPropertyTranslation(mTestProblem.timeQFunction);
			System.out.println("State-reward DTMC Translation (with QAs):");
			System.out.println(dtmcWithQAs);
			System.out.println();
			System.out.println("Time Query Property Translation:");
			System.out.println(timeQueryTranslation);
			System.out.println();
		} catch (ActionDefinitionNotFoundException | EffectClassNotFoundException | VarNotFoundException
				| ActionNotFoundException | DiscriminantNotFoundException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XDTMC to PRISM DTMC");
		}
	}

	@Test
	public void testPrismDTMCTranslatorTransitionReward() throws XMDPException {
		XDTMC xdtmc = mTestProblem.createXDTMC();

		try {
			PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true,
					PrismRewardType.TRANSITION_REWARD);
			String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);
			String timeQueryTranslation = dtmcTranslator.getNumQueryPropertyTranslation(mTestProblem.timeQFunction);
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

	@Test
	public void testPrismMDPAdversaryGeneration() throws XMDPException {
		String outputPath = "/Users/rsukkerd/Projects/explainable-planning/models/test0/output";
		String staOutputFilename = "adv.sta";
		String traOutputFilename = "adv.tra";
		String labOutputFilename = "adv.lab";
		String srewOutputFilename = "adv.srew";
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath,
				staOutputFilename, traOutputFilename, labOutputFilename, srewOutputFilename);

		XMDP xmdp = mTestProblem.createXMDP();
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

	@Test
	public void testPrismExplicitDTMCPropertyQuery() {
		String modelPath = "/Users/rsukkerd/Projects/explainable-planning/models/test0/test_output";
		String staFilename = "adv.sta";
		String traFilename = "adv.tra";
		String labFilename = "adv.lab";
		String srewFilename = "adv.srew";
		PrismExplicitModelPointer explicitModelPointer = new PrismExplicitModelPointer(modelPath, staFilename,
				traFilename, labFilename, srewFilename);
		String propertyStr = "R=? [ F rLoc=0 & readyToCopy ]";

		// Default PRISM configuration
		PrismConfiguration prismConfig = new PrismConfiguration();

		try {
			PrismAPIWrapper prismAPI = new PrismAPIWrapper(prismConfig);
			double totalCost = prismAPI.queryPropertyFromExplicitDTMC(propertyStr, explicitModelPointer, 1);
			double totalTime = prismAPI.queryPropertyFromExplicitDTMC(propertyStr, explicitModelPointer, 2);
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

	@Test
	public void testPrismDTMCPropertyQuery() throws XMDPException {
		XDTMC xdtmc = mTestProblem.createXDTMC();
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);
		String timeQuery = dtmcTranslator.getNumQueryPropertyTranslation(mTestProblem.timeQFunction);

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

}
