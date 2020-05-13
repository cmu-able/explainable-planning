package main;

import java.io.File;
import java.io.IOException;

import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.demo.MobileRobotXMDPLoader;
import examples.mobilerobot.demo.MobileRobotXPlanner;
import explanation.verbalization.VerbalizerSettings;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.objectives.CostCriterion;
import models.explanation.HPolicyExplanation;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;
import whynot.IWhyNotXPlanner;
import whynot.WhyNotXPlanner;

public class MobileRobotWhyNotXPlanner implements IWhyNotXPlanner {

	private static final String MOBILEROBOT_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/mobilerobot";
	private static final String PROBLEMS_PATH = MOBILEROBOT_PATH + "/missions";
	private static final String POLICIES_PATH = MOBILEROBOT_PATH + "/policies";
	private static final String MAPS_PATH = MOBILEROBOT_PATH + "/maps";

	private WhyNotXPlanner mWhyNotXPlanner;

	public MobileRobotWhyNotXPlanner(File mapsJsonDir, XPlannerOutDirectories outputDirs,
			VerbalizerSettings verbalizerSettings) {
		IXMDPLoader xmdpLoader = new MobileRobotXMDPLoader(mapsJsonDir);
		mWhyNotXPlanner = new WhyNotXPlanner(xmdpLoader, outputDirs, MobileRobotXPlanner.getVocabulary(),
				verbalizerSettings);
	}

	@Override
	public HPolicyExplanation answerWhyNotQuery(File missionJsonFile, File queryPolicyJsonFile, String whyNotQueryStr)
			throws DSMException, XMDPException, PrismConnectorException, IOException, ParseException, PrismException,
			GRBException {
		return mWhyNotXPlanner.answerWhyNotQuery(missionJsonFile, CostCriterion.TOTAL_COST, queryPolicyJsonFile,
				whyNotQueryStr);
	}

	public static void main(String[] args) throws IOException, DSMException, XMDPException, PrismConnectorException,
			ParseException, PrismException, GRBException {
		String problemFilename = args[0];
		String queryPolicyFilename = args[1];
		String whyNotQueryStr = args[2];

		File problemFile = new File(PROBLEMS_PATH, problemFilename);
		File queryPolicyJsonFile = new File(POLICIES_PATH, queryPolicyFilename);
		File mapsJsonDir = new File(MAPS_PATH);

		XPlannerOutDirectories outputDirs = WhyNotXPlanner.getDefaultXPlannerOutDirectories();
		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs

		MobileRobotWhyNotXPlanner whyNotXPlanner = new MobileRobotWhyNotXPlanner(mapsJsonDir, outputDirs,
				defaultVerbalizerSettings);
		whyNotXPlanner.answerWhyNotQuery(problemFile, queryPolicyJsonFile, whyNotQueryStr);
	}

}
