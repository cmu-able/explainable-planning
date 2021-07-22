package examples.dart.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;

import examples.clinicscheduling.demo.ClinicSchedulingXPlanner;
import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.PlannerArguments;
import examples.common.XPlanner;
import examples.common.XPlannerOutDirectories;
import examples.dart.metrics.DestroyedProbabilityQFunction;
import examples.dart.metrics.MissTargetEvent;
import examples.dart.viz.DartPolicyViz;
import explanation.analysis.PolicyInfo;
import explanation.rendering.GenericTextBasedPolicyRenderer;
import explanation.rendering.IExplanationRenderer;
import explanation.rendering.IPolicyRenderer;
import explanation.rendering.TextBasedExplanationRenderer;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.PrismConnectorException;
import solver.prismconnector.exceptions.ResultParsingException;

public class DartXPlanner {

	private XPlanner mXPlanner;

	public DartXPlanner(XPlannerOutDirectories outputDirs, VerbalizerSettings verbalizerSettings) {
		IXMDPLoader xmdpLoader = new DartXMDPLoader();
		mXPlanner = new XPlanner(xmdpLoader, outputDirs, getVocabulary(), verbalizerSettings);
	}

	public PolicyInfo runXPlanning(File problemFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		return mXPlanner.runXPlanning(problemFile, CostCriterion.TOTAL_COST);
	}

	public PolicyInfo runPlanning(File problemFile) throws DSMException, XMDPException, PrismException, IOException,
			ResultParsingException, ExplicitModelParsingException, GRBException {
		return mXPlanner.runPlanning(problemFile, CostCriterion.TOTAL_COST);
	}

	public static void main(String[] args)
			throws IOException, PrismException, XMDPException, PrismConnectorException, GRBException, DSMException {
		Properties arguments = PlannerArguments.parsePlanningCommandLineArguments(DartXPlanner.class.getSimpleName(), args);
		File problemFile = new File(arguments.getProperty(PlannerArguments.PROBLEM_FILES_PATH_PROP), arguments.getProperty(PlannerArguments.PROBLEM_FILE_PROP));

	
		XPlannerOutDirectories outputDirs = new XPlannerOutDirectories(arguments);

		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs
		DartXPlanner xplanner = new DartXPlanner(outputDirs, defaultVerbalizerSettings);
		xplanner.runXPlanning(problemFile);
		
		// Generate explanation in text
		IPolicyRenderer policyRenderer = new DartPolicyViz(problemFile);
		IExplanationRenderer xplanRenderer = new TextBasedExplanationRenderer(policyRenderer);
		String problem = arguments.getProperty(PlannerArguments.PROBLEM_FILE_PROP);
		String explanationFile = FilenameUtils.getBaseName(problem) + "_explanation.json";
		xplanRenderer.renderExplanation(Paths.get(arguments.getProperty(XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH_PROP), explanationFile).toString());
	}

	public static Vocabulary getVocabulary() {
		Vocabulary vocab = new Vocabulary();
		// E[#targets missed]
		vocab.putNoun(MissTargetEvent.NAME, "expected number of targets missed");
		vocab.putVerb(MissTargetEvent.NAME, "miss");
		vocab.putUnit(MissTargetEvent.NAME, "expected target", "expected targets");
		vocab.setOmitUnitWhenNounPresent(MissTargetEvent.NAME);

		// Prob(being destroyed)
		vocab.putNoun(DestroyedProbabilityQFunction.NAME, "probability of being destroyed");
		vocab.putVerb(DestroyedProbabilityQFunction.NAME, "have");
		vocab.putUnit(DestroyedProbabilityQFunction.NAME, "probaility of being destroyed", null);
		vocab.setOmitUnitWhenNounPresent(DestroyedProbabilityQFunction.NAME);
		return vocab;
	}
}
