package examples.mobilerobot.demo;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.metrics.CollisionDomain;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.IntrusivenessDomain;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.qfactors.MoveToAction;
import explanation.analysis.Explainer;
import explanation.analysis.Explanation;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.metrics.CountQFunction;
import language.metrics.NonStandardMetricQFunction;
import language.objectives.CostCriterion;
import language.policy.Policy;
import prism.PrismException;
import solver.prismconnector.PrismConfiguration;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.ExplanationWriter;

public class MobileRobotDemo {
	static final String MAPS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/maps";
	static final String MISSIONS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/missions";
	static final String PRISM_MODELS_OUTPUT_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/tmpdata/prism/models";
	static final String PRISM_ADVS_OUTPUT_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/tmpdata/prism/advs";
	static final String POLICIES_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/tmpdata/policies";
	static final String EXPLANATIONS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/tmpdata/explanations";

	private MobileRobotXMDPLoader mXMDPLoader;
	private PrismConfiguration mPrismConfig = new PrismConfiguration();

	public MobileRobotDemo() {
		mXMDPLoader = new MobileRobotXMDPLoader(MAPS_PATH, MISSIONS_PATH);
	}

	public void run(File missionJsonFile) throws PrismException, IOException, ResultParsingException, XMDPException,
			ExplicitModelParsingException, GRBException, ParseException, MapTopologyException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		String modelOutputPath = PRISM_MODELS_OUTPUT_PATH + "/" + missionName;
		String advOutputPath = PRISM_ADVS_OUTPUT_PATH + "/" + missionName;

		XMDP xmdp = mXMDPLoader.loadXMDP(missionJsonFile);

		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath,
				mPrismConfig);
		PrismConnector prismConn = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSetttings);
		Policy policy = prismConn.generateOptimalPolicy();

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConn.terminate();

		Explainer explainer = new Explainer(prismConnSetttings);
		Explanation explanation = explainer.explain(xmdp, CostCriterion.TOTAL_COST, policy);

		Vocabulary vocabulary = getVocabulary(xmdp);
		Verbalizer verbalizer = new Verbalizer(vocabulary, POLICIES_PATH + "/" + missionName);

		String explanationJsonFilename = String.format("%s_explanation.json", missionName);
		ExplanationWriter explanationWriter = new ExplanationWriter(EXPLANATIONS_PATH, verbalizer);
		File explanationJsonFile = explanationWriter.writeExplanation(missionName, explanation,
				explanationJsonFilename);

		System.out.println("Explanation JSON file: " + explanationJsonFile.getAbsolutePath());
	}

	public static void main(String[] args) throws ResultParsingException, PrismException, IOException, XMDPException,
			ExplicitModelParsingException, GRBException, ParseException, MapTopologyException {
		String missionFilename = args[0];
		File missionJsonFile = new File(MISSIONS_PATH, missionFilename);

		MobileRobotDemo demo = new MobileRobotDemo();
		demo.run(missionJsonFile);
	}

	public static Vocabulary getVocabulary(XMDP xmdp) {
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
