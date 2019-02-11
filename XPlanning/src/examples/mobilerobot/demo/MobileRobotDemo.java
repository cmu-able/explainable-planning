package examples.mobilerobot.demo;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.Directories;
import examples.mobilerobot.metrics.CollisionDomain;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.IntrusivenessDomain;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.models.MoveToAction;
import explanation.analysis.Explainer;
import explanation.analysis.Explanation;
import explanation.analysis.PolicyInfo;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.domain.metrics.CountQFunction;
import language.domain.metrics.NonStandardMetricQFunction;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.ExplanationWriter;

public class MobileRobotDemo {
	public static final String MAPS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/mobilerobot/maps";
	public static final String MISSIONS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/mobilerobot/missions";

	private MobileRobotXMDPLoader mXMDPLoader;

	public MobileRobotDemo(String mapJsonDirPath, String missionJsonDirPath) {
		mXMDPLoader = new MobileRobotXMDPLoader(mapJsonDirPath, missionJsonDirPath);
	}

	public void run(File missionJsonFile) throws PrismException, IOException, ResultParsingException, XMDPException,
			ExplicitModelParsingException, GRBException, DSMException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		String modelOutputPath = Directories.PRISM_MODELS_OUTPUT_PATH + "/" + missionName;
		String advOutputPath = Directories.PRISM_ADVS_OUTPUT_PATH + "/" + missionName;

		XMDP xmdp = mXMDPLoader.loadXMDP(missionJsonFile);

		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSetttings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConnector.terminate();

		Explainer explainer = new Explainer(prismConnSetttings);
		Explanation explanation = explainer.explain(xmdp, CostCriterion.TOTAL_COST, policyInfo);

		Vocabulary vocabulary = getVocabulary(xmdp);
		VerbalizerSettings verbalizerSettings = new VerbalizerSettings();
		Verbalizer verbalizer = new Verbalizer(vocabulary, CostCriterion.TOTAL_COST,
				Directories.POLICIES_OUTPUT_PATH + "/" + missionName, verbalizerSettings);

		String explanationJsonFilename = String.format("%s_explanation.json", missionName);
		ExplanationWriter explanationWriter = new ExplanationWriter(Directories.EXPLANATIONS_OUTPUT_PATH, verbalizer);
		File explanationJsonFile = explanationWriter.writeExplanation(missionName, explanation,
				explanationJsonFilename);

		System.out.println("Explanation JSON file: " + explanationJsonFile.getAbsolutePath());
	}

	public static void main(String[] args) throws ResultParsingException, PrismException, IOException, XMDPException,
			ExplicitModelParsingException, GRBException, ParseException, DSMException {
		String missionFilename = args[0];
		File missionJsonFile = new File(MISSIONS_PATH, missionFilename);

		MobileRobotDemo demo = new MobileRobotDemo(MAPS_PATH, MISSIONS_PATH);
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