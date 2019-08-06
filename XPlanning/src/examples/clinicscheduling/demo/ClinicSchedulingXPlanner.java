package examples.clinicscheduling.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import examples.clinicscheduling.metrics.IdleTimeQFunction;
import examples.clinicscheduling.metrics.LeadTimeQFunction;
import examples.clinicscheduling.metrics.OvertimeQFunction;
import examples.clinicscheduling.metrics.RevenueQFunction;
import examples.clinicscheduling.metrics.SwitchABPQFunction;
import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.common.XPlanner;
import examples.common.XPlannerOutDirectories;
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

public class ClinicSchedulingXPlanner {

	public static final String PROBLEMS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/clinicscheduling/missions";
	public static final int DEFAULT_BRANCH_FACTOR = 3;

	private XPlanner mXPlanner;

	public ClinicSchedulingXPlanner(int branchFactor, XPlannerOutDirectories outputDirs,
			VerbalizerSettings verbalizerSettings) {
		IXMDPLoader xmdpLoader = new ClinicSchedulingXMDPLoader(branchFactor);
		mXPlanner = new XPlanner(xmdpLoader, outputDirs, getVocabulary(), verbalizerSettings);
	}

	public PolicyInfo runXPlanning(File missionJsonFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		return mXPlanner.runXPlanning(missionJsonFile, CostCriterion.AVERAGE_COST);
	}

	public PolicyInfo runPlanning(File missionJsonFile) throws DSMException, XMDPException, PrismException, IOException,
			ResultParsingException, ExplicitModelParsingException, GRBException {
		return mXPlanner.runPlanning(missionJsonFile, CostCriterion.AVERAGE_COST);
	}

	public static void main(String[] args)
			throws PrismException, XMDPException, IOException, GRBException, DSMException, PrismConnectorException {
		String problemFilename = args[0];
		File problemFile = new File(PROBLEMS_PATH, problemFilename);

		Path policiesOutputPath = Paths.get(XPlannerOutDirectories.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(XPlannerOutDirectories.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(XPlannerOutDirectories.PRISM_OUTPUT_PATH);
		XPlannerOutDirectories outputDirs = new XPlannerOutDirectories(policiesOutputPath, explanationOutputPath,
				prismOutputPath);

		VerbalizerSettings defaultVerbalizerSettings = new VerbalizerSettings(); // describe costs
		ClinicSchedulingXPlanner demo = new ClinicSchedulingXPlanner(DEFAULT_BRANCH_FACTOR, outputDirs,
				defaultVerbalizerSettings);
		demo.runXPlanning(problemFile);
	}

	public static Vocabulary getVocabulary() {
		Vocabulary vocab = new Vocabulary();
		vocab.putNoun(RevenueQFunction.NAME, "revenue");
		vocab.putVerb(RevenueQFunction.NAME, "have");
		vocab.putUnit(RevenueQFunction.NAME, "dollar in revenue", "dollars in revenue");
		vocab.putNoun(OvertimeQFunction.NAME, "overtime cost");
		vocab.putVerb(OvertimeQFunction.NAME, "have");
		vocab.putUnit(OvertimeQFunction.NAME, "dollar in overtime cost", "dollars in overtime cost");
		vocab.putNoun(IdleTimeQFunction.NAME, "idle time cost");
		vocab.putVerb(IdleTimeQFunction.NAME, "have");
		vocab.putUnit(IdleTimeQFunction.NAME, "dollar in idle time cost", "dollars in idle time cost");
		vocab.putNoun(LeadTimeQFunction.NAME, "appointment lead time cost");
		vocab.putVerb(LeadTimeQFunction.NAME, "have");
		vocab.putUnit(LeadTimeQFunction.NAME, "dollar in appointment lead time cost",
				"dollars in appointment lead time cost");
		vocab.putNoun(SwitchABPQFunction.NAME, "switching ABP cost");
		vocab.putVerb(SwitchABPQFunction.NAME, "have");
		vocab.putUnit(SwitchABPQFunction.NAME, "dollar in switching ABP cost", "dollars in switching ABP cost");
		vocab.setPeriodUnit("day");
		return vocab;
	}

}
