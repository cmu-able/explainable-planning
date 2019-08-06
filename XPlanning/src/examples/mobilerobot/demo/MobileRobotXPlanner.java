package examples.mobilerobot.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.XPlanner;
import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import explanation.analysis.PolicyInfo;
import explanation.verbalization.QADecimalFormatter;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;

public class MobileRobotXPlanner {
	public static final String MAPS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/mobilerobot/maps";
	public static final String MISSIONS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/mobilerobot/missions";

	private XPlanner mXPlanner;

	public MobileRobotXPlanner(File mapsJsonDir, XPlannerOutDirectories outputDirs, VerbalizerSettings verbalizerSettings) {
		IXMDPLoader xmdpLoader = new MobileRobotXMDPLoader(mapsJsonDir);
		mXPlanner = new XPlanner(xmdpLoader, outputDirs, getVocabulary(), verbalizerSettings);
	}

	public XMDP loadXMDPFromMissionFile(File missionJsonFile) throws DSMException, XMDPException {
		return mXPlanner.loadXMDPFromProblemFile(missionJsonFile);
	}

	public PolicyInfo runXPlanning(File missionJsonFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		return mXPlanner.runXPlanning(missionJsonFile, CostCriterion.TOTAL_COST);
	}

	public PolicyInfo runPlanning(File missionJsonFile) throws DSMException, XMDPException, PrismException, IOException,
			ResultParsingException, ExplicitModelParsingException, GRBException {
		return mXPlanner.runPlanning(missionJsonFile, CostCriterion.TOTAL_COST);
	}

	public static void main(String[] args)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		String missionFilename = args[0];
		File missionJsonFile = new File(MISSIONS_PATH, missionFilename);
		File mapsJsonDir = new File(MAPS_PATH);

		Path policiesOutputPath = Paths.get(XPlannerOutDirectories.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(XPlannerOutDirectories.PRISM_OUTPUT_PATH);
		XPlannerOutDirectories outputDirs = new XPlannerOutDirectories(policiesOutputPath, explanationOutputPath,
				prismOutputPath);

		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs
		MobileRobotXPlanner xplanner = new MobileRobotXPlanner(mapsJsonDir, outputDirs, defaultVerbalizerSettings);
		xplanner.runXPlanning(missionJsonFile);
	}

	public static Vocabulary getVocabulary() {
		Vocabulary vocab = new Vocabulary();
		vocab.putNoun(TravelTimeQFunction.NAME, "travel time");
		vocab.putVerb(TravelTimeQFunction.NAME, "take");
		vocab.putUnit(TravelTimeQFunction.NAME, "minute", "minutes");
		vocab.putNoun(CollisionEvent.NAME, "collision");
		vocab.putVerb(CollisionEvent.NAME, "have");
		vocab.putUnit(CollisionEvent.NAME, "expected collision", "expected collisions");
		vocab.setOmitUnitWhenNounPresent(CollisionEvent.NAME);
		vocab.putNoun(IntrusiveMoveEvent.NAME, "intrusiveness");
		vocab.putVerb(IntrusiveMoveEvent.NAME, "be");
		vocab.putCategoricalValue(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.NON_INTRUSIVE_EVENT_NAME,
				"non-intrusive");
		vocab.putCategoricalValue(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.SOMEWHAT_INTRUSIVE_EVENT_NAME,
				"somewhat-intrusive");
		vocab.putCategoricalValue(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.VERY_INTRUSIVE_EVENT_NAME,
				"very-intrusive");
		vocab.putPreposition(IntrusiveMoveEvent.NAME, "at");
		vocab.putUnit(IntrusiveMoveEvent.NAME, "location", "locations");
		return vocab;
	}

	public static QADecimalFormatter getQADecimalFormatter() {
		QADecimalFormatter decimalFormatter = new QADecimalFormatter();
		decimalFormatter.putDecimalFormat(TravelTimeQFunction.NAME, "#");
		decimalFormatter.putDecimalFormat(CollisionEvent.NAME, "#.#");
		decimalFormatter.putDecimalFormat(IntrusiveMoveEvent.NAME, "#");
		return decimalFormatter;
	}

	public static void setVerbalizerOrdering(VerbalizerSettings verbalizerSettings) {
		verbalizerSettings.appendQFunctionName(TravelTimeQFunction.NAME);
		verbalizerSettings.appendQFunctionName(CollisionEvent.NAME);
		verbalizerSettings.appendQFunctionName(IntrusiveMoveEvent.NAME);
		verbalizerSettings.appendEventName(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.NON_INTRUSIVE_EVENT_NAME);
		verbalizerSettings.appendEventName(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.SOMEWHAT_INTRUSIVE_EVENT_NAME);
		verbalizerSettings.appendEventName(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.VERY_INTRUSIVE_EVENT_NAME);
	}

}