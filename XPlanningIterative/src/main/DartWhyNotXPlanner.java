package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.XPlannerOutDirectories;
import examples.dart.demo.DartXMDPLoader;
import examples.dart.demo.DartXPlanner;
import explanation.verbalization.VerbalizerSettings;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.objectives.CostCriterion;
import models.explanation.HPolicyExplanation;
import prism.PrismException;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;

public class DartWhyNotXPlanner {

	private static final String DART_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/dart";
	private static final String PROBLEMS_PATH = DART_PATH + "/missions";
	private static final String POLICIES_PATH = DART_PATH + "/policies";

	private WhyNotXPlanner mWhyNotXPlanner;

	public DartWhyNotXPlanner(XPlannerOutDirectories outputDirs, VerbalizerSettings verbalizerSettings) {
		IXMDPLoader xmdpLoader = new DartXMDPLoader();
		mWhyNotXPlanner = new WhyNotXPlanner(xmdpLoader, outputDirs, DartXPlanner.getVocabulary(), verbalizerSettings);
	}

	public HPolicyExplanation answerWhyNotQuery(File problemFile, File queryPolicyJsonFile, String whyNotQueryStr)
			throws DSMException, XMDPException, IOException, ParseException, ResultParsingException, PrismException,
			ExplicitModelParsingException, GRBException {
		return mWhyNotXPlanner.answerWhyNotQuery(problemFile, CostCriterion.TOTAL_COST, queryPolicyJsonFile,
				whyNotQueryStr);
	}

	public static void main(String[] args) throws IOException, ResultParsingException, ExplicitModelParsingException,
			DSMException, XMDPException, ParseException, PrismException, GRBException {
		String problemFilename = args[0];
		String queryPolicyFilename = args[1];
		String whyNotQueryStr = args[2];

		File problemFile = new File(PROBLEMS_PATH, problemFilename);
		File queryPolicyJsonFile = new File(POLICIES_PATH, queryPolicyFilename);

		Path policiesOutputPath = Paths.get(WhyNotXPlanner.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(WhyNotXPlanner.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(WhyNotXPlanner.PRISM_OUTPUT_PATH);
		XPlannerOutDirectories outputDirs = new XPlannerOutDirectories(policiesOutputPath, explanationOutputPath,
				prismOutputPath);

		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs
		DartWhyNotXPlanner whyNotXPlanner = new DartWhyNotXPlanner(outputDirs, defaultVerbalizerSettings);
		whyNotXPlanner.answerWhyNotQuery(problemFile, queryPolicyJsonFile, whyNotQueryStr);
	}

}
