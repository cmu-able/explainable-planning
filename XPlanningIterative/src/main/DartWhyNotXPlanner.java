package main;

import java.io.File;
import java.io.IOException;

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
import models.explanation.WhyNotQuery;
import prism.PrismException;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;

public class DartWhyNotXPlanner {

	private WhyNotXPlanner mWhyNotXPlanner;

	public DartWhyNotXPlanner(XPlannerOutDirectories outputDirs, VerbalizerSettings verbalizerSettings) {
		IXMDPLoader xmdpLoader = new DartXMDPLoader();
		mWhyNotXPlanner = new WhyNotXPlanner(xmdpLoader, outputDirs, DartXPlanner.getVocabulary(), verbalizerSettings);
	}

	public HPolicyExplanation answerWhyNotQuery(File problemFile, File queryPolicyJsonFile,
			WhyNotQuery<?, ?> whyNotQuery) throws DSMException, XMDPException, IOException, ParseException,
			ResultParsingException, PrismException, ExplicitModelParsingException, GRBException {
		return mWhyNotXPlanner.answerWhyNotQuery(problemFile, CostCriterion.TOTAL_COST, queryPolicyJsonFile,
				whyNotQuery);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
