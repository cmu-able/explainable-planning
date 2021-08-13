package examples.mobilerobot.tests;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.common.DSMException;
import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.demo.MobileRobotXPlanner;
import examples.mobilerobot.demo.MobileRobotXMDPLoader;
import examples.utils.SimpleConsoleLogger;
import examples.utils.XMDPDataProvider;
import explanation.analysis.Explainer;
import explanation.analysis.ExplainerSettings;
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
		String modelOutputPath = "../../../../data/mobilerobots/policies/" + missionName;
		String advOutputPath = "../../../../data/mobilerobots/policies/" + missionName;
		PrismConnectorSettings prismConnSettings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSettings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConnector.terminate();

		ExplainerSettings explainerSettings = new ExplainerSettings(prismConnSettings);
		Explainer explainer = new Explainer(explainerSettings);
		Explanation explanation = explainer.explain(xmdp, CostCriterion.TOTAL_COST, policyInfo);

		Vocabulary vocabulary = MobileRobotXPlanner.getVocabulary();
		VerbalizerSettings verbalizerSettings = new VerbalizerSettings();
		File policyJsonDir = new File("../../../../data/mobilerobots/policies/" + missionName);
		Verbalizer verbalizer = new Verbalizer(vocabulary, CostCriterion.TOTAL_COST, policyJsonDir, verbalizerSettings);
		String verbalization = verbalizer.verbalize(explanation);

		SimpleConsoleLogger.log("Explanation", verbalization, false);
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] loadXMDPs() throws XMDPException, DSMException {
		String mapsJsonDirPath = "../../../../data/mobilerobots/maps/";
		String missionsJsonDirPath = "../../../../data/mobilerobots/missions/";
		File mapsJsonDir = new File(mapsJsonDirPath);

		MobileRobotXMDPLoader testLoader = new MobileRobotXMDPLoader(mapsJsonDir);
		return XMDPDataProvider.loadXMDPs(missionsJsonDirPath, testLoader);
	}

	@BeforeMethod
	public void printMissionFilename(Object[] data) {
		File missionJsonFile = (File) data[0];
		SimpleConsoleLogger.log("Mission", missionJsonFile.getName(), true);
	}
}