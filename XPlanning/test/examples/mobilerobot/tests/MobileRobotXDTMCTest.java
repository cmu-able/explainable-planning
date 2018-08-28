package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import language.dtmc.XDTMC;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.metrics.IQFunction;
import language.policy.Policy;
import prism.PrismException;
import solver.prismconnector.PrismAPIWrapper;
import solver.prismconnector.PrismConfiguration;
import solver.prismconnector.PrismDTMCTranslator;
import solver.prismconnector.PrismMDPTranslator;
import solver.prismconnector.PrismPropertyTranslator;
import solver.prismconnector.PrismRewardType;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PolicyReader;

public class MobileRobotXDTMCTest {

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismDTMCTranslatorStateReward(PolicyReader explicitDTMCReader, XDTMC xdtmc) {
		try {
			PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
			String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);
			SimpleConsoleLogger.log("State-reward DTMC Translation (with QAs)", dtmcWithQAs, false);
			SimpleConsoleLogger.newLine();

			for (IQFunction<?, ?> qFunction : xdtmc.getXMDP().getQSpace()) {
				String queryTranslation = dtmcTranslator.getNumQueryPropertyTranslation(qFunction);
				SimpleConsoleLogger.log("Query Property Translation of " + qFunction.getName(), queryTranslation,
						false);
				SimpleConsoleLogger.newLine();
			}
		} catch (XMDPException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XDTMC to PRISM DTMC");
		}
	}

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismDTMCTranslatorTransitionReward(PolicyReader explicitDTMCReader, XDTMC xdtmc) {
		try {
			PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true,
					PrismRewardType.TRANSITION_REWARD);
			String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);
			SimpleConsoleLogger.log("Transition-reward DTMC Translation (with QAs)", dtmcWithQAs, false);
			SimpleConsoleLogger.newLine();

			for (IQFunction<?, ?> qFunction : xdtmc.getXMDP().getQSpace()) {
				String queryTranslation = dtmcTranslator.getNumQueryPropertyTranslation(qFunction);
				SimpleConsoleLogger.log("Query Property Translation of " + qFunction.getName(), queryTranslation,
						false);
				SimpleConsoleLogger.newLine();
			}
		} catch (XMDPException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XDTMC to PRISM DTMC");
		}
	}

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismExplicitDTMCPropertyQuery(PolicyReader explicitDTMCReader, XDTMC xdtmc)
			throws VarNotFoundException {
		ValueEncodingScheme encodings = explicitDTMCReader.getValueEncodingScheme();
		PrismPropertyTranslator propTranslator = new PrismPropertyTranslator(encodings);
		String propertyStr = propTranslator.buildDTMCRawRewardQueryProperty(xdtmc.getXMDP().getGoal());
		PrismExplicitModelPointer explicitDTMCPointer = explicitDTMCReader.getPrismExplicitModelPointer();

		// Default PRISM configuration
		PrismConfiguration prismConfig = new PrismConfiguration();

		try {
			PrismAPIWrapper prismAPI = new PrismAPIWrapper(prismConfig);
			double totalCost = prismAPI.queryPropertyFromExplicitDTMC(propertyStr, explicitDTMCPointer, 1);
			List<Double> totalValues = prismAPI.queryPropertiesFromExplicitDTMC(propertyStr, explicitDTMCPointer);

			SimpleConsoleLogger.log("Query values from explicit DTMC file...");
			SimpleConsoleLogger.log("Query property", propertyStr, true);
			SimpleConsoleLogger.log("Expected total cost of adversary", totalCost, true);
			for (int i = 0; i < totalValues.size(); i++) {
				int rewIndex = i + 1;
				SimpleConsoleLogger.log("Expected total value " + rewIndex + " of adversary", totalValues.get(i), true);
			}
			SimpleConsoleLogger.newLine();

			// Close down PRISM
			prismAPI.terminatePrism();
		} catch (PrismException | ResultParsingException e) {
			e.printStackTrace();
			fail("Exception thrown while PRISM model checking DTCM property");
		}
	}

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismDTMCPropertyQuery(PolicyReader explicitDTMCReader, XDTMC xdtmc)
			throws XMDPException {
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true, PrismRewardType.STATE_REWARD);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true);

		// Default PRISM configuration
		PrismConfiguration prismConfig = new PrismConfiguration();

		try {
			for (IQFunction<?, ?> qFunction : xdtmc.getXMDP().getQSpace()) {
				String query = dtmcTranslator.getNumQueryPropertyTranslation(qFunction);
				PrismAPIWrapper prismAPI = new PrismAPIWrapper(prismConfig);
				double result = prismAPI.queryPropertyFromDTMC(dtmcWithQAs, query);

				SimpleConsoleLogger.log("Query Property", query, true);
				SimpleConsoleLogger.log("Expected total " + qFunction.getName(), result, true);
				SimpleConsoleLogger.newLine();

				// Close down PRISM
				prismAPI.terminatePrism();
			}
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
			PolicyReader explicitDTMCReader = generateAdverary(missionJsonFile, xmdp);
			Policy policy = explicitDTMCReader.readPolicyFromFiles();
			XDTMC xdtmc = new XDTMC(xmdp, policy);
			data[i] = new Object[] { explicitDTMCReader, xdtmc };
			i++;
		}
		return data;
	}

	private PolicyReader generateAdverary(File missionJsonFile, XMDP xmdp)
			throws XMDPException, PrismException, ResultParsingException, IOException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		String outputPath = MobileRobotXMDPTest.PRISM_OUTPUT_PATH + "/" + missionName;
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath, "model",
				PrismRewardType.STATE_REWARD);

		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true, PrismRewardType.STATE_REWARD);
		String mdpWithQAs = mdpTranslator.getMDPTranslation(true);
		String goalProperty = mdpTranslator.getGoalPropertyTranslation();

		// Default PRISM configuration
		PrismConfiguration prismConfig = new PrismConfiguration();

		PrismAPIWrapper prismAPI = new PrismAPIWrapper(prismConfig);
		prismAPI.generateMDPAdversary(mdpWithQAs, goalProperty, outputExplicitModelPointer);

		PolicyReader explicitDTMCReader = new PolicyReader(
				mdpTranslator.getValueEncodingScheme(), outputExplicitModelPointer);

		// Close down PRISM
		prismAPI.terminatePrism();
		return explicitDTMCReader;
	}

	@BeforeMethod
	public void printExplicitModelDirName(Object[] data) {
		PolicyReader explicitDTMCReader = (PolicyReader) data[0];
		PrismExplicitModelPointer explicitDTMCPointer = explicitDTMCReader.getPrismExplicitModelPointer();
		SimpleConsoleLogger.log("Adversary", explicitDTMCPointer.getExplicitModelDirectory().getName(), true);
	}
}
