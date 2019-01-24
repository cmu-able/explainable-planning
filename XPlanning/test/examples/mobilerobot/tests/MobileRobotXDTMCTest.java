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

import examples.common.DSMException;
import examples.common.Directories;
import examples.mobilerobot.demo.MobileRobotXMDPLoader;
import examples.utils.SimpleConsoleLogger;
import language.domain.metrics.IQFunction;
import language.dtmc.XDTMC;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import language.policy.Policy;
import prism.PrismException;
import solver.prismconnector.PrismAPIWrapper;
import solver.prismconnector.PrismDTMCTranslator;
import solver.prismconnector.PrismMDPTranslator;
import solver.prismconnector.PrismPropertyTranslator;
import solver.prismconnector.PrismRewardType;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class MobileRobotXDTMCTest {

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismDTMCTranslatorTransitionReward(PrismExplicitModelReader explicitDTMCReader, XDTMC xdtmc) {
		try {
			PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc);
			String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true, false);
			SimpleConsoleLogger.log("Transition-reward DTMC Translation (with QAs)", dtmcWithQAs, false);
			SimpleConsoleLogger.newLine();

			for (IQFunction<?, ?> qFunction : xdtmc.getXMDP().getQSpace()) {
				String queryTranslation = dtmcTranslator.getNumQueryPropertyTranslation(qFunction,
						CostCriterion.TOTAL_COST);
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
	public void testPrismExplicitDTMCPropertyQuery(PrismExplicitModelReader explicitDTMCReader, XDTMC xdtmc)
			throws VarNotFoundException {
		ValueEncodingScheme encodings = explicitDTMCReader.getValueEncodingScheme();
		PrismPropertyTranslator propTranslator = new PrismPropertyTranslator(encodings);
		String propertyStr = propTranslator.buildDTMCRawRewardQueryProperty(xdtmc.getXMDP().getGoal(),
				CostCriterion.TOTAL_COST);
		PrismExplicitModelPointer explicitDTMCPointer = explicitDTMCReader.getPrismExplicitModelPointer();

		try {
			PrismAPIWrapper prismAPI = new PrismAPIWrapper();
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
		} catch (UnsupportedOperationException e) {
			SimpleConsoleLogger.log("Numerical query from explicit model not supported");
		}
	}

	@Test(dataProvider = "xdtmcSolutions")
	public void testPrismDTMCPropertyQuery(PrismExplicitModelReader explicitDTMCReader, XDTMC xdtmc)
			throws XMDPException {
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true, false);

		try {
			for (IQFunction<?, ?> qFunction : xdtmc.getXMDP().getQSpace()) {
				String query = dtmcTranslator.getNumQueryPropertyTranslation(qFunction, CostCriterion.TOTAL_COST);
				PrismAPIWrapper prismAPI = new PrismAPIWrapper();
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
	public Object[][] generateAdversaries()
			throws IOException, ParseException, ResultParsingException, XMDPException, PrismException, DSMException {
		String mapJsonDirPath = MobileRobotXMDPTest.MAPS_PATH;
		String missionJsonDirPath = MobileRobotXMDPTest.MISSIONS_PATH;

		MobileRobotXMDPLoader testLoader = new MobileRobotXMDPLoader(mapJsonDirPath, missionJsonDirPath);
		File missionJsonDir = new File(missionJsonDirPath);
		File[] missionJsonFiles = missionJsonDir.listFiles();
		Object[][] data = new Object[missionJsonFiles.length][2];

		int i = 0;
		for (File missionJsonFile : missionJsonFiles) {
			XMDP xmdp = testLoader.loadXMDP(missionJsonFile);
			PrismExplicitModelReader explicitDTMCReader = generateAdverary(missionJsonFile, xmdp);
			Policy policy = explicitDTMCReader.readPolicyFromFiles();
			XDTMC xdtmc = new XDTMC(xmdp, policy);
			data[i] = new Object[] { explicitDTMCReader, xdtmc };
			i++;
		}
		return data;
	}

	private PrismExplicitModelReader generateAdverary(File missionJsonFile, XMDP xmdp)
			throws XMDPException, PrismException, ResultParsingException, IOException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		String outputPath = Directories.PRISM_ADVS_OUTPUT_PATH + "/" + missionName;
		PrismExplicitModelPointer outputExplicitModelPointer = new PrismExplicitModelPointer(outputPath, "model",
				PrismRewardType.TRANSITION_REWARD);

		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp);
		String mdpWithQAs = mdpTranslator.getMDPTranslation(true);
		String goalProperty = mdpTranslator.getGoalPropertyTranslation(CostCriterion.TOTAL_COST);

		PrismAPIWrapper prismAPI = new PrismAPIWrapper();
		prismAPI.generateMDPAdversary(mdpWithQAs, goalProperty, outputExplicitModelPointer);

		PrismExplicitModelReader explicitDTMCReader = new PrismExplicitModelReader(outputExplicitModelPointer,
				mdpTranslator.getValueEncodingScheme());

		// Close down PRISM
		prismAPI.terminatePrism();
		return explicitDTMCReader;
	}

	@BeforeMethod
	public void printExplicitModelDirName(Object[] data) {
		PrismExplicitModelReader explicitDTMCReader = (PrismExplicitModelReader) data[0];
		PrismExplicitModelPointer explicitDTMCPointer = explicitDTMCReader.getPrismExplicitModelPointer();
		SimpleConsoleLogger.log("Adversary", explicitDTMCPointer.getExplicitModelDirectory().getName(), true);
	}
}
