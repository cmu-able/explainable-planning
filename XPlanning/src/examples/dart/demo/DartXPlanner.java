package examples.dart.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.XPlanner;
import examples.common.XPlannerOutDirectories;
import examples.dart.metrics.DestroyedProbabilityQFunction;
import examples.dart.metrics.MissTargetEvent;
import explanation.analysis.PolicyInfo;
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

	public static final String PROBLEMS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/dart/missions";

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
		String problemFilename = args[0];
		File problemFile = new File(PROBLEMS_PATH, problemFilename);

		Path policiesOutputPath = Paths.get(XPlannerOutDirectories.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(XPlannerOutDirectories.PRISM_OUTPUT_PATH);
		XPlannerOutDirectories outputDirs = new XPlannerOutDirectories(policiesOutputPath, explanationOutputPath,
				prismOutputPath);

		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs
		DartXPlanner xplanner = new DartXPlanner(outputDirs, defaultVerbalizerSettings);
		xplanner.runXPlanning(problemFile);
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
