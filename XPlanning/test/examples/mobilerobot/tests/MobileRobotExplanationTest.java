package examples.mobilerobot.tests;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.common.Directories;
import examples.mobilerobot.demo.MobileRobotDemo;
import examples.mobilerobot.demo.MobileRobotXMDPLoader;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import explanation.analysis.Explainer;
import explanation.analysis.Explanation;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import language.policy.Policy;
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
		PrismConnector prismConn = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSetttings);
		Policy policy = prismConn.generateOptimalPolicy();

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConn.terminate();

		Explainer explainer = new Explainer(prismConnSetttings);
		Explanation explanation = explainer.explain(xmdp, CostCriterion.TOTAL_COST, policy);

		Vocabulary vocabulary = MobileRobotDemo.getVocabulary(xmdp);
		VerbalizerSettings verbalizerSettings = new VerbalizerSettings();
		Verbalizer verbalizer = new Verbalizer(vocabulary, CostCriterion.TOTAL_COST,
				Directories.POLICIES_OUTPUT_PATH + "/" + missionName, verbalizerSettings);
		String verbalization = verbalizer.verbalize(explanation);

		SimpleConsoleLogger.log("Explanation", verbalization, false);
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] loadXMDPs() throws IOException, ParseException, MapTopologyException, XMDPException {
		String mapJsonDirPath = MobileRobotXMDPTest.MAPS_PATH;
		String missionJsonDirPath = MobileRobotXMDPTest.MISSIONS_PATH;

		MobileRobotXMDPLoader testLoader = new MobileRobotXMDPLoader(mapJsonDirPath, missionJsonDirPath);
		File missionJsonDir = new File(missionJsonDirPath);
		File[] missionJsonFiles = missionJsonDir.listFiles();
		Object[][] data = new Object[missionJsonFiles.length][2];

		int i = 0;
		for (File missionJsonFile : missionJsonFiles) {
			XMDP xmdp = testLoader.loadXMDP(missionJsonFile);
			data[i] = new Object[] { missionJsonFile, xmdp };
			i++;
		}
		return data;
	}

	@BeforeMethod
	public void printMissionFilename(Object[] data) {
		File missionJsonFile = (File) data[0];
		SimpleConsoleLogger.log("Mission", missionJsonFile.getName(), true);
	}
}