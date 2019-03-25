package examples.clinicscheduling.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import examples.clinicscheduling.metrics.IdleTimeQFunction;
import examples.clinicscheduling.metrics.LeadTimeQFunction;
import examples.clinicscheduling.metrics.OvertimeQFunction;
import examples.clinicscheduling.metrics.RevenueQFunction;
import examples.clinicscheduling.metrics.SwitchABPQFunction;
import examples.common.DSMException;
import examples.common.Directories;
import explanation.analysis.Explainer;
import explanation.analysis.Explanation;
import explanation.analysis.PolicyInfo;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.VerbalizerSettings;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.gurobiconnector.GRBConnectorSettings;
import solver.gurobiconnector.GRBSolverUtils;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;
import uiconnector.ExplanationWriter;

public class ClinicSchedulingDemo {

	public static final String PROBLEMS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/clinicscheduling/missions";
	public static final int DEFAULT_BRANCH_FACTOR = 3;

	private ClinicSchedulingXMDPLoader mXMDPLoader;
	private Directories mOutputDirs;

	public ClinicSchedulingDemo(String problemsPath, int branchFactor, Directories outputDirs) {
		mXMDPLoader = new ClinicSchedulingXMDPLoader(problemsPath, branchFactor);
		mOutputDirs = outputDirs;
	}

	public void run(File problemFile) throws PrismException, XMDPException, IOException, ExplicitModelParsingException,
			GRBException, DSMException, ResultParsingException {
		String problemName = FilenameUtils.removeExtension(problemFile.getName());
		Path modelOutputPath = mOutputDirs.getPrismModelsOutputPath().resolve(problemName);
		Path advOutputPath = mOutputDirs.getPrismAdvsOutputPath().resolve(problemName);

		XMDP xmdp = mXMDPLoader.loadXMDP(problemFile);

		// Use PrismConnector to export XMDP to explicit model files
		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath.toString(),
				advOutputPath.toString());
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.AVERAGE_COST, prismConnSetttings);
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConnector.terminate();

		// GRBConnector reads from explicit model files
		GRBConnectorSettings grbConnSettings = new GRBConnectorSettings(prismExplicitModelReader,
				GRBSolverUtils.DEFAULT_FEASIBILITY_TOL, GRBSolverUtils.DEFAULT_INT_FEAS_TOL);
		GRBConnector grbConnector = new GRBConnector(xmdp, CostCriterion.AVERAGE_COST, grbConnSettings);
		PolicyInfo policyInfo = grbConnector.generateOptimalPolicy();

		Explainer explainer = new Explainer(prismConnSetttings);
		Explanation explanation = explainer.explain(xmdp, CostCriterion.AVERAGE_COST, policyInfo);

		Vocabulary vocabulary = getVocabulary(xmdp);
		VerbalizerSettings verbalizerSettings = new VerbalizerSettings();
		Path policyJsonPath = mOutputDirs.getPoliciesOutputPath().resolve(problemName);
		Verbalizer verbalizer = new Verbalizer(vocabulary, CostCriterion.AVERAGE_COST, policyJsonPath.toString(),
				verbalizerSettings);

		String explanationJsonFilename = String.format("%s_explanation.json", problemName);
		Path explanationOutputPath = mOutputDirs.getExplanationOutputPath();
		ExplanationWriter explanationWriter = new ExplanationWriter(explanationOutputPath.toString(), verbalizer);
		File explanationJsonFile = explanationWriter.writeExplanation(problemName, explanation,
				explanationJsonFilename);

		System.out.println("Explanation JSON file: " + explanationJsonFile.getAbsolutePath());
	}

	public static void main(String[] args) throws PrismException, XMDPException, IOException,
			ExplicitModelParsingException, GRBException, DSMException, ResultParsingException {
		String problemFilename = args[0];
		File problemFile = new File(PROBLEMS_PATH, problemFilename);

		Path policiesOutputPath = Paths.get(Directories.POLICIES_OUTPUT_PATH);
		Path explanationOutputPath = Paths.get(Directories.EXPLANATIONS_OUTPUT_PATH);
		Path prismOutputPath = Paths.get(Directories.PRISM_OUTPUT_PATH);
		Directories outputDirs = new Directories(policiesOutputPath, explanationOutputPath, prismOutputPath);

		ClinicSchedulingDemo demo = new ClinicSchedulingDemo(PROBLEMS_PATH, DEFAULT_BRANCH_FACTOR, outputDirs);
		demo.run(problemFile);
	}

	public static Vocabulary getVocabulary(XMDP xmdp) {
		QSpace qSpace = xmdp.getQSpace();

		RevenueQFunction revenueQFunction = qSpace.getQFunction(RevenueQFunction.class, RevenueQFunction.NAME);
		OvertimeQFunction overtimeQFunction = qSpace.getQFunction(OvertimeQFunction.class, OvertimeQFunction.NAME);
		IdleTimeQFunction idleTimeQFunction = qSpace.getQFunction(IdleTimeQFunction.class, IdleTimeQFunction.NAME);
		LeadTimeQFunction leadTimeQFunction = qSpace.getQFunction(LeadTimeQFunction.class, LeadTimeQFunction.NAME);
		SwitchABPQFunction switchABPQFunction = qSpace.getQFunction(SwitchABPQFunction.class, SwitchABPQFunction.NAME);

		Vocabulary vocab = new Vocabulary();
		vocab.putNoun(revenueQFunction, "revenue");
		vocab.putVerb(revenueQFunction, "have");
		vocab.putUnit(revenueQFunction, "dollar in revenue", "dollars in revenue");
		vocab.putNoun(overtimeQFunction, "overtime cost");
		vocab.putVerb(overtimeQFunction, "have");
		vocab.putUnit(overtimeQFunction, "dollar in overtime cost", "dollars in overtime cost");
		vocab.putNoun(idleTimeQFunction, "idle time cost");
		vocab.putVerb(idleTimeQFunction, "have");
		vocab.putUnit(idleTimeQFunction, "dollar in idle time cost", "dollars in idle time cost");
		vocab.putNoun(leadTimeQFunction, "appointment lead time cost");
		vocab.putVerb(leadTimeQFunction, "have");
		vocab.putUnit(leadTimeQFunction, "dollar in appointment lead time cost",
				"dollars in appointment lead time cost");
		vocab.putNoun(switchABPQFunction, "switching ABP cost");
		vocab.putVerb(switchABPQFunction, "have");
		vocab.putUnit(switchABPQFunction, "dollar in switching ABP cost", "dollars in switching ABP cost");
		vocab.setPeriodUnit("day");

		return vocab;
	}

}
