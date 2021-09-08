package examples.mobilerobot.demo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.PlannerArguments;
import examples.common.XPlanner;
import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.EnergyConsumptionQFunction;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.viz.MapBasedPolicyRenderer;
import explanation.analysis.PolicyInfo;
import explanation.rendering.IExplanationRenderer;
import explanation.rendering.IPolicyRenderer;
import explanation.rendering.TextBasedExplanationRenderer;
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
// java -cp "target/classes:lib/*" examples.mobilerobot.demo.MobileRobotXPlanner --props data/docker-globals.properties --props data/mobilerobot/docker-problem-spec.properties mission0.json

public class MobileRobotXPlanner {
	public static final String MAPS_PATH_PROP = "Maps.Path";

	private XPlanner mXPlanner;

	public MobileRobotXPlanner(File mapsJsonDir, XPlannerOutDirectories outputDirs,
			VerbalizerSettings verbalizerSettings) {
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
		Properties arguments = PlannerArguments.parsePlanningCommandLineArguments(MobileRobotXPlanner.class.getSimpleName(), args);
		File problemFile = new File(arguments.getProperty(PlannerArguments.PROBLEM_FILES_PATH_PROP), arguments.getProperty(PlannerArguments.PROBLEM_FILE_PROP));
		File mapsJsonDir = new File(arguments.getProperty(MAPS_PATH_PROP));
	
		XPlannerOutDirectories outputDirs = new XPlannerOutDirectories(arguments);

		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs
		defaultVerbalizerSettings.setQADecimalFormatter(getQADecimalFormatter());
		setVerbalizerOrdering(defaultVerbalizerSettings);
		MobileRobotXPlanner xplanner = new MobileRobotXPlanner(mapsJsonDir, outputDirs, defaultVerbalizerSettings);
		System.out.println("Running the planner and generating explanation...");
		// Reassign Sytem.out to a file to prevent trace output of planning
		PrintStream console = System.out;
		System.setOut(new PrintStream(new File("planning-trace.log")));
		xplanner.runXPlanning(problemFile);
		System.setOut(console);
		
		// Generate explanation in text
		int consoleWidth = -1;
		if (arguments.containsKey("consoleWidth")) consoleWidth = (Integer )arguments.get("consoleWidth");
		IPolicyRenderer policyRenderer = new MapBasedPolicyRenderer(mapsJsonDir, problemFile, consoleWidth);
		IExplanationRenderer xplanRenderer = new TextBasedExplanationRenderer(policyRenderer, consoleWidth);
		String problem = arguments.getProperty(PlannerArguments.PROBLEM_FILE_PROP);
		String explanationFile = FilenameUtils.getBaseName(problem) + "_explanation.json";
		xplanRenderer.renderExplanation(Paths.get(arguments.getProperty(XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH_PROP), explanationFile).toString());
	}

	public static Vocabulary getVocabulary() {
		Vocabulary vocab = new Vocabulary();
		vocab.putNoun(TravelTimeQFunction.NAME, "travel time");
		vocab.putVerb(TravelTimeQFunction.NAME, "take");
		vocab.putUnit(TravelTimeQFunction.NAME, "second", "seconds");
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
		
		vocab.putNoun(EnergyConsumptionQFunction.NAME, "energy");
		vocab.putVerb(EnergyConsumptionQFunction.NAME, "consumes");
		vocab.putUnit(EnergyConsumptionQFunction.NAME, "milliwatt", "milliwatts");
		
		return vocab;
	}

	public static QADecimalFormatter getQADecimalFormatter() {
		QADecimalFormatter decimalFormatter = new QADecimalFormatter();
		decimalFormatter.putDecimalFormat(TravelTimeQFunction.NAME, "#");
		decimalFormatter.putDecimalFormat(CollisionEvent.NAME, "#.#");
		decimalFormatter.putDecimalFormat(IntrusiveMoveEvent.NAME, "#");
		decimalFormatter.putDecimalFormat(EnergyConsumptionQFunction.NAME, "#");
		return decimalFormatter;
	}

	public static void setVerbalizerOrdering(VerbalizerSettings verbalizerSettings) {
		verbalizerSettings.appendQFunctionName(TravelTimeQFunction.NAME);
		verbalizerSettings.appendQFunctionName(CollisionEvent.NAME);
		verbalizerSettings.appendQFunctionName(IntrusiveMoveEvent.NAME);
		verbalizerSettings.appendQFunctionName(EnergyConsumptionQFunction.NAME);

		verbalizerSettings.appendEventName(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.NON_INTRUSIVE_EVENT_NAME);
		verbalizerSettings.appendEventName(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.SOMEWHAT_INTRUSIVE_EVENT_NAME);
		verbalizerSettings.appendEventName(IntrusiveMoveEvent.NAME, IntrusiveMoveEvent.VERY_INTRUSIVE_EVENT_NAME);
		
		
	}

}