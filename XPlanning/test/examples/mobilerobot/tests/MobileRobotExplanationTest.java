package examples.mobilerobot.tests;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.common.DSMException;
import examples.common.Directories;
import examples.mobilerobot.demo.MobileRobotDemo;
import examples.mobilerobot.demo.MobileRobotXMDPLoader;
import examples.utils.SimpleConsoleLogger;
import examples.utils.XMDPDataProvider;
import explanation.analysis.Explainer;
import explanation.analysis.Explanation;
import explanation.analysis.PolicyInfo;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;

public class MobileRobotExplanationTest {

	@Test(dataProvider = "xmdpProblems")
	public void testContrastiveJustification(File missionJsonFile, XMDP xmdp) throws PrismException,
			ResultParsingException, XMDPException, IOException, ExplicitModelParsingException, GRBException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		String modelOutputPath = Directories.PRISM_MODELS_OUTPUT_PATH + "/" + missionName;
		String advOutputPath = Directories.PRISM_ADVS_OUTPUT_PATH + "/" + missionName;
		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSetttings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConnector.terminate();

		Explainer explainer = new Explainer(prismConnSetttings);
		Explanation explanation = explainer.explain(xmdp, CostCriterion.TOTAL_COST, policyInfo);

		Vocabulary vocabulary = MobileRobotDemo.getVocabulary(xmdp);
		VerbalizerSettings verbalizerSettings = new VerbalizerSettings();
		Verbalizer verbalizer = new Verbalizer(vocabulary, CostCriterion.TOTAL_COST,
				Directories.POLICIES_OUTPUT_PATH + "/" + missionName, verbalizerSettings);
		String verbalization = verbalizer.verbalize(explanation);

		SimpleConsoleLogger.log("Explanation", verbalization, false);
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] loadXMDPs() throws XMDPException, DSMException {
		String mapJsonDirPath = MobileRobotDemo.MAPS_PATH;
		String missionJsonDirPath = MobileRobotDemo.MISSIONS_PATH;

		MobileRobotXMDPLoader testLoader = new MobileRobotXMDPLoader(mapJsonDirPath, missionJsonDirPath);
		return XMDPDataProvider.loadXMDPs(missionJsonDirPath, testLoader);
	}

	@BeforeMethod
	public void printMissionFilename(Object[] data) {
		File missionJsonFile = (File) data[0];
		SimpleConsoleLogger.log("Mission", missionJsonFile.getName(), true);
	}
}