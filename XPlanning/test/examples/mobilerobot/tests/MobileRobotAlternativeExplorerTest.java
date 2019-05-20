package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.common.DSMException;
import examples.common.XPlanningOutDirectories;
import examples.mobilerobot.demo.MobileRobotXMDPLoader;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.utils.SimpleConsoleLogger;
import explanation.analysis.AlternativeExplorer;
import explanation.analysis.PolicyInfo;
import gurobi.GRBException;
import language.domain.metrics.IQFunction;
import language.dtmc.XDTMC;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.gurobiconnector.GRBConnectorSettings;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.PrismDTMCTranslator;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;

public class MobileRobotAlternativeExplorerTest {

	@Test(dataProvider = "xmdpProblems")
	public void testGenerateTravelTimeAlternative(File missionJsonFile, GRBConnector grbConnector,
			PolicyInfo policyInfo) {
		AlternativeExplorer altExplorer = new AlternativeExplorer(grbConnector);

		try {
			IQFunction<?, ?> qFunction = policyInfo.getXMDP().getQSpace().getQFunction(TravelTimeQFunction.class,
					TravelTimeQFunction.NAME);

			PolicyInfo altPolicyInfo = altExplorer.getParetoOptimalAlternative(policyInfo, qFunction);

			if (altPolicyInfo == null) {
				SimpleConsoleLogger
						.log(String.format("No alternative found that improves %s.", TravelTimeQFunction.NAME));
			} else {
				SimpleConsoleLogger.logHeader(String.format("Alternative that improves %s", TravelTimeQFunction.NAME));
				printPrismDTMCAndProperties(altPolicyInfo);
			}

			SimpleConsoleLogger.newLine();
		} catch (XMDPException | PrismException | ResultParsingException | IOException | ExplicitModelParsingException
				| GRBException e) {
			e.printStackTrace();
			fail("Exception thrown while findign an alternative");
		}
	}

	@Test(dataProvider = "xmdpProblems")
	public void testAlternativeExplorer(File missionJsonFile, GRBConnector grbConnector, PolicyInfo policyInfo) {
		AlternativeExplorer altExplorer = new AlternativeExplorer(grbConnector);

		try {
			Set<PolicyInfo> altPolicies = altExplorer.getParetoOptimalAlternatives(policyInfo);
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

			for (PolicyInfo altPolicyInfo : altPolicies) {
				printPrismDTMCAndProperties(altPolicyInfo);
				SimpleConsoleLogger.newLine();
			}
		} catch (ResultParsingException | XMDPException | PrismException | IOException | ExplicitModelParsingException
				| GRBException e) {
			e.printStackTrace();
			fail("Exception thrown while finding alternative policies");
		}
	}

	private void printPrismDTMCAndProperties(PolicyInfo policyInfo)
			throws XMDPException, ResultParsingException, PrismException {
		XDTMC xdtmc = new XDTMC(policyInfo.getXMDP(), policyInfo.getPolicy());
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslation(true, false);

		SimpleConsoleLogger.log("Transition-reward DTMC Translation (with QAs)", dtmcWithQAs, false);
		SimpleConsoleLogger.newLine();

		for (IQFunction<?, ?> qFunction : xdtmc.getXMDP().getQSpace()) {
			double qaValue = policyInfo.getQAValue(qFunction);

			SimpleConsoleLogger.log(String.format("QA value of %s", qFunction.getName()), qaValue, true);
		}
	}

	@BeforeMethod
	public void printMissionFilename(Object[] data) throws ResultParsingException, XMDPException, PrismException {
		File missionJsonFile = (File) data[0];
		PolicyInfo policyInfo = (PolicyInfo) data[2];

		SimpleConsoleLogger.log("Mission", missionJsonFile.getName(), true);
		SimpleConsoleLogger.logHeader("Solution Policy");
		printPrismDTMCAndProperties(policyInfo);
		SimpleConsoleLogger.newLine();
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] loadXMDPs() throws IOException, PrismException, ResultParsingException, XMDPException,
			DSMException, ExplicitModelParsingException {
		String mapsJsonDirPath = MobileRobotXMDPTest.MAPS_PATH;
		String missionsJsonDirPath = MobileRobotXMDPTest.MISSIONS_PATH;
		File mapsJsonDir = new File(mapsJsonDirPath);

		MobileRobotXMDPLoader testLoader = new MobileRobotXMDPLoader(mapsJsonDir);
		File missionJsonDir = new File(missionsJsonDirPath);
		File[] missionJsonFiles = missionJsonDir.listFiles();
		Object[][] data = new Object[missionJsonFiles.length][3];

		int i = 0;
		for (File missionJsonFile : missionJsonFiles) {
			// XMDP problem
			XMDP xmdp = testLoader.loadXMDP(missionJsonFile);

			// PrismConnector
			String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
			String modelOutputPath = XPlanningOutDirectories.PRISM_MODELS_OUTPUT_PATH + "/" + missionName;
			String advOutputPath = XPlanningOutDirectories.PRISM_ADVS_OUTPUT_PATH + "/" + missionName;
			PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
			PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSetttings);

			// GRBConnector
			PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
			ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();
			PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
					encodings);
			GRBConnectorSettings grbConnSettings = new GRBConnectorSettings(prismExplicitModelReader);
			GRBConnector grbConnector = new GRBConnector(xmdp, CostCriterion.TOTAL_COST, grbConnSettings);

			// Compute optimal policy
			PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

			// Close down PRISM
			prismConnector.terminate();

			// Pass mission.json, GRB connector, and policy info as data
			data[i] = new Object[] { missionJsonFile, grbConnector, policyInfo };
			i++;
		}
		return data;
	}
}
