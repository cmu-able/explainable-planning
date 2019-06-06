package examples.mobilerobot.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import examples.common.DSMException;
import examples.common.XPlanningOutDirectories;
import examples.mobilerobot.metrics.CollisionDomain;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.IntrusivenessDomain;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.models.MoveToAction;
import explanation.analysis.DifferenceScaler;
import explanation.analysis.Explainer;
import explanation.analysis.ExplainerSettings;
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
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;
import uiconnector.ExplanationWriter;

public class MobileRobotDemo {
	public static final String MAPS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/mobilerobot/maps";
	public static final String MISSIONS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/mobilerobot/missions";

	private MobileRobotXMDPLoader mXMDPLoader;
	private XPlanningOutDirectories mOutputDirs;

	public MobileRobotDemo(File mapsJsonDir, XPlanningOutDirectories outputDirs) {
		mXMDPLoader = new MobileRobotXMDPLoader(mapsJsonDir);
		mOutputDirs = outputDirs;
	}

	public void runXPlanning(File missionJsonFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		runXPlanning(missionJsonFile, null);
	}

	public void runXPlanning(File missionJsonFile, DifferenceScaler diffScaler)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		Path modelOutputPath = mOutputDirs.getPrismModelsOutputPath().resolve(missionName);
		Path advOutputPath = mOutputDirs.getPrismAdvsOutputPath().resolve(missionName);

		XMDP xmdp = mXMDPLoader.loadXMDP(missionJsonFile);

		PrismConnectorSettings prismConnSettings = new PrismConnectorSettings(modelOutputPath.toString(),
				advOutputPath.toString());
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSettings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConnector.terminate();

		// ExplainerSettings define what DifferenceScaler to use, if any
		ExplainerSettings explainerSettings = new ExplainerSettings(prismConnSettings);
		explainerSettings.setDifferenceScaler(diffScaler);
		Explainer explainer = new Explainer(explainerSettings);
		Explanation explanation = explainer.explain(xmdp, CostCriterion.TOTAL_COST, policyInfo);

		Vocabulary vocabulary = getVocabulary(xmdp.getQSpace());
		VerbalizerSettings verbalizerSettings = new VerbalizerSettings();
		setDecimalFormats(verbalizerSettings, xmdp.getQSpace());
		Path policyJsonPath = mOutputDirs.getPoliciesOutputPath().resolve(missionName);
		Verbalizer verbalizer = new Verbalizer(vocabulary, CostCriterion.TOTAL_COST, policyJsonPath.toFile(),
				verbalizerSettings);

		String explanationJsonFilename = String.format("%s_explanation.json", missionName);
		Path explanationOutputPath = mOutputDirs.getExplanationsOutputPath();
		ExplanationWriter explanationWriter = new ExplanationWriter(explanationOutputPath.toFile(), verbalizer);
		File explanationJsonFile = explanationWriter.writeExplanation(missionJsonFile.getName(), explanation,
				explanationJsonFilename);

		System.out.println("Explanation JSON file: " + explanationJsonFile.getAbsolutePath());
	}

	public PolicyInfo runPlanning(File missionJsonFile)
			throws DSMException, XMDPException, PrismException, IOException, ResultParsingException {
		String missionName = FilenameUtils.removeExtension(missionJsonFile.getName());
		Path modelOutputPath = mOutputDirs.getPrismModelsOutputPath().resolve(missionName);
		Path advOutputPath = mOutputDirs.getPrismAdvsOutputPath().resolve(missionName);

		XMDP xmdp = mXMDPLoader.loadXMDP(missionJsonFile);

		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath.toString(),
				advOutputPath.toString());
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.TOTAL_COST, prismConnSetttings);
		PolicyInfo policyInfo = prismConnector.generateOptimalPolicy();

		// Close down PRISM
		prismConnector.terminate();

		return policyInfo;
	}

	public static void main(String[] args)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		String missionFilename = args[0];
		File missionJsonFile = new File(MISSIONS_PATH, missionFilename);
		File mapsJsonDir = new File(MAPS_PATH);

		Path policiesOutputPath = Paths.get(XPlanningOutDirectories.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(XPlanningOutDirectories.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(XPlanningOutDirectories.PRISM_OUTPUT_PATH);
		XPlanningOutDirectories outputDirs = new XPlanningOutDirectories(policiesOutputPath, explanationOutputPath,
				prismOutputPath);

		MobileRobotDemo demo = new MobileRobotDemo(mapsJsonDir, outputDirs);
		demo.runXPlanning(missionJsonFile);
	}

	public static Vocabulary getVocabulary(QSpace qSpace) {
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

	public static void setDecimalFormats(VerbalizerSettings verbalizerSettings, QSpace qSpace) {
		TravelTimeQFunction timeQFunction = qSpace.getQFunction(TravelTimeQFunction.class, TravelTimeQFunction.NAME);
		CountQFunction<MoveToAction, CollisionDomain, CollisionEvent> collideQFunction = qSpace
				.getQFunction(CountQFunction.class, CollisionEvent.NAME);
		NonStandardMetricQFunction<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> intrusiveQFunction = qSpace
				.getQFunction(NonStandardMetricQFunction.class, IntrusiveMoveEvent.NAME);

		verbalizerSettings.putDecimalFormat(timeQFunction, "#");
		verbalizerSettings.putDecimalFormat(collideQFunction, "#.#");
		verbalizerSettings.putDecimalFormat(intrusiveQFunction, "#");
	}

}