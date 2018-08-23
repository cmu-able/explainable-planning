package examples.mobilerobot.tests;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.metrics.CollisionDomain;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.IntrusivenessDomain;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.qfactors.MoveToAction;
import explanation.analysis.Explainer;
import explanation.verbalization.Vocabulary;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.metrics.CountQFunction;
import language.metrics.NonStandardMetricQFunction;
import language.policy.Policy;
import prism.PrismException;
import prismconnector.PrismConfiguration;
import prismconnector.PrismConnector;
import prismconnector.PrismConnectorSettings;
import prismconnector.exceptions.ResultParsingException;

public class MobileRobotExplanationTest {

	static final String POLICY_JSON_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/policies";

	@Test(dataProvider = "xmdpProblems")
	public void testContrastiveJustification(File missionJsonFile, XMDP xmdp)
			throws PrismException, ResultParsingException, XMDPException, IOException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		String outputPath = MobileRobotXMDPTest.PRISM_OUTPUT_PATH + "/" + missionName;
		PrismConfiguration prismConfig = new PrismConfiguration();
		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(false, outputPath, prismConfig);
		PrismConnector prismConn = new PrismConnector(xmdp, prismConnSetttings);
		Policy policy = prismConn.generateOptimalPolicy();

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConn.terminate();

		Vocabulary vocabulary = getVocabulary(xmdp);
		Explainer explainer = new Explainer(prismConnSetttings, vocabulary, POLICY_JSON_PATH);
		String explanation = explainer.explain(missionName, xmdp, policy);

		SimpleConsoleLogger.log("Explanation", explanation, false);
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] loadXMDPs() throws IOException, ParseException, MapTopologyException, XMDPException {
		String mapJsonDirPath = MobileRobotXMDPTest.MAPS_PATH;
		String missionJsonDirPath = MobileRobotXMDPTest.MISSIONS_PATH;

		MobileRobotTestLoader testLoader = new MobileRobotTestLoader(mapJsonDirPath, missionJsonDirPath);
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

	public Vocabulary getVocabulary(XMDP xmdp) {
		QSpace qSpace = xmdp.getQSpace();
		TravelTimeQFunction timeQFunction = qSpace.getQFunction(TravelTimeQFunction.class, TravelTimeQFunction.NAME);
		CountQFunction<MoveToAction, CollisionDomain, CollisionEvent> collideQFunction = qSpace
				.getQFunction(CountQFunction.class, CollisionEvent.NAME);
		NonStandardMetricQFunction<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> intrusiveQFunction = qSpace
				.getQFunction(NonStandardMetricQFunction.class, IntrusiveMoveEvent.NAME);

		Vocabulary vocab = new Vocabulary();
		vocab.putNoun(timeQFunction, "time");
		vocab.putVerb(timeQFunction, "take");
		vocab.putUnit(timeQFunction, "minute", "minutes");
		vocab.putNoun(collideQFunction, "collision");
		vocab.putVerb(collideQFunction, "have");
		vocab.putUnit(collideQFunction, "collision", "collisions");
		vocab.putNoun(intrusiveQFunction, "intrusiveness");
		vocab.putVerb(intrusiveQFunction, "be");
		for (IntrusiveMoveEvent event : intrusiveQFunction.getEventBasedMetric().getEvents()) {
			vocab.putCategoricalValue(intrusiveQFunction, event, event.getName());
		}
		vocab.putUnit(intrusiveQFunction, "step", "steps");
		return vocab;
	}
}
