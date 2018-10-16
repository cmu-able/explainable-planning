package examples.clinicscheduling.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;

import examples.clinicscheduling.dsm.ClinicCostProfile;
import examples.clinicscheduling.dsm.ClinicSchedulingXMDPBuilder;
import examples.clinicscheduling.dsm.SchedulingContext;
import examples.clinicscheduling.metrics.IdleTimeQFunction;
import examples.clinicscheduling.metrics.LeadTimeQFunction;
import examples.clinicscheduling.metrics.OvertimeQFunction;
import examples.clinicscheduling.metrics.RevenueQFunction;
import examples.clinicscheduling.metrics.SwitchABPQFunction;
import examples.common.Directories;
import explanation.analysis.Explainer;
import explanation.analysis.Explanation;
import explanation.verbalization.Verbalizer;
import explanation.verbalization.Vocabulary;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.objectives.CostCriterion;
import language.policy.Policy;
import prism.PrismException;
import solver.gurobiconnector.GRBConnector;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.ValueEncodingScheme;
import solver.prismconnector.exceptions.ExplicitModelParsingException;
import solver.prismconnector.exceptions.ResultParsingException;
import solver.prismconnector.explicitmodel.PrismExplicitModelPointer;
import solver.prismconnector.explicitmodel.PrismExplicitModelReader;
import uiconnector.ExplanationWriter;

public class ClinicSchedulingDemo {

	private static final String MISSIONS_PATH = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/clinicscheduling/missions";

	private static final int DEFAULT_BRANCH_FACTOR = 3;

	private static final String CAPACITY_PARAM = "capacity";
	private static final String MAX_ABP_PARAM = "maxABP";
	private static final String MAX_QUEUE_SIZE_PARAM = "maxQueueSize";
	private static final String REVENUE_PARAM = "revenuePerPatient";
	private static final String OVERTIME_COST_PARAM = "overtimeCostPerPatient";
	private static final String IDLE_TIME_COST_PARAM = "idleTimeCostPerPatient";
	private static final String LEAD_TIME_COST_PARAM = "leadTimeCostFactor";
	private static final String SWITCH_ABP_COST_PARAM = "switchABPCostFactor";
	private static final String INI_ABP_PARAM = "iniABP";
	private static final String INI_AB_COUNT_PARAM = "iniABCount";
	private static final String INI_NEW_CLIENT_COUNT_PARAM = "iniNewClientCount";
	private static final String CLIENT_ARRIVAL_RATE_PARAM = "clientArrivalRate";

	private ClinicSchedulingXMDPBuilder mXMDPBuilder;

	public ClinicSchedulingDemo(int branchFactor) {
		mXMDPBuilder = new ClinicSchedulingXMDPBuilder(branchFactor);
	}

	public void run(String missionName, SchedulingContext schedulingContext, int iniABP, int iniABCount,
			int iniNewClientCount, double clientArrivalRate) throws PrismException, XMDPException, IOException,
			ExplicitModelParsingException, GRBException, ResultParsingException {
		XMDP xmdp = mXMDPBuilder.buildXMDP(schedulingContext, iniABP, iniABCount, iniNewClientCount, clientArrivalRate);

		String modelOutputPath = Directories.PRISM_MODELS_OUTPUT_PATH + "/" + missionName;
		String advOutputPath = Directories.PRISM_ADVS_OUTPUT_PATH + "/" + missionName;

		// Use PrismConnector to export XMDP to explicit model files
		PrismConnectorSettings prismConnSetttings = new PrismConnectorSettings(modelOutputPath, advOutputPath);
		PrismConnector prismConnector = new PrismConnector(xmdp, CostCriterion.AVERAGE_COST, prismConnSetttings);
		PrismExplicitModelPointer prismExplicitModelPtr = prismConnector.exportExplicitModelFiles();
		ValueEncodingScheme encodings = prismConnector.getPrismMDPTranslator().getValueEncodingScheme();
		PrismExplicitModelReader prismExplicitModelReader = new PrismExplicitModelReader(prismExplicitModelPtr,
				encodings);

		// Close down PRISM -- before explainer creates a new PrismConnector
		prismConnector.terminate();

		// GRBConnector reads from explicit model files
		GRBConnector grbConnector = new GRBConnector(xmdp, CostCriterion.AVERAGE_COST, prismExplicitModelReader);
		Policy policy = grbConnector.generateOptimalPolicy();

		Explainer explainer = new Explainer(prismConnSetttings);
		Explanation explanation = explainer.explain(xmdp, CostCriterion.AVERAGE_COST, policy);

		Vocabulary vocabulary = getVocabulary(xmdp);
		Verbalizer verbalizer = new Verbalizer(vocabulary, CostCriterion.AVERAGE_COST,
				Directories.POLICIES_OUTPUT_PATH + "/" + missionName);

		String explanationJsonFilename = String.format("%s_explanation.json", missionName);
		ExplanationWriter explanationWriter = new ExplanationWriter(Directories.EXPLANATIONS_OUTPUT_PATH, verbalizer);
		File explanationJsonFile = explanationWriter.writeExplanation(missionName, explanation,
				explanationJsonFilename);

		System.out.println("Explanation JSON file: " + explanationJsonFile.getAbsolutePath());
	}

	public static void main(String[] args) throws ParseException, ResultParsingException, PrismException, XMDPException,
			IOException, ExplicitModelParsingException, GRBException {
		String missionFilename = args[0];
		File missionFile = new File(MISSIONS_PATH, missionFilename);
		String missionName = FilenameUtils.removeExtension(missionFile.getName());
		String argsLine = null;

		try (FileReader fileReader = new FileReader(missionFile);
				BufferedReader buffReader = new BufferedReader(fileReader);) {
			argsLine = buffReader.readLine();
		}

		String[] argsArray = argsLine.split(" ");

		CommandLineParser parser = new DefaultParser();

		Options options = new Options();
		options.addOption("C", CAPACITY_PARAM, true, "Capacity of the clinic");
		options.addOption("M", MAX_ABP_PARAM, true, "Maximum ABP");
		options.addOption("N", MAX_QUEUE_SIZE_PARAM, true, "Maximum queue size");
		options.addOption("R", REVENUE_PARAM, true, "Revenue per patient");
		options.addOption("O", OVERTIME_COST_PARAM, true, "Overtime cost per patient");
		options.addOption("I", IDLE_TIME_COST_PARAM, true, "Idle time cost per patient");
		options.addOption("L", LEAD_TIME_COST_PARAM, true, "Lead time cost factor");
		options.addOption("S", SWITCH_ABP_COST_PARAM, true, "Switching ABP cost factor");
		options.addOption("w", INI_ABP_PARAM, true, "Initial ABP");
		options.addOption("x", INI_AB_COUNT_PARAM, true, "Initial number of advance-booking patients");
		options.addOption("y", INI_NEW_CLIENT_COUNT_PARAM, true, "Initial number of new patients");
		options.addOption("l", CLIENT_ARRIVAL_RATE_PARAM, true, "Average patient arrival rate");

		CommandLine line = parser.parse(options, argsArray);
		int capacity = Integer.parseInt(line.getOptionValue(CAPACITY_PARAM, "10"));
		int maxABP = Integer.parseInt(line.getOptionValue(MAX_ABP_PARAM, "15"));
		int maxQueueSize = Integer.parseInt(line.getOptionValue(MAX_QUEUE_SIZE_PARAM, "150"));
		double revenuePerPatient = Double.parseDouble(line.getOptionValue(REVENUE_PARAM, "20"));
		double overtimeCostPerPatient = Double.parseDouble(line.getOptionValue(OVERTIME_COST_PARAM, "10"));
		double idleTimeCostPerPatient = Double.parseDouble(line.getOptionValue(IDLE_TIME_COST_PARAM, "0"));
		double leadTimeCostFactor = Double.parseDouble(line.getOptionValue(LEAD_TIME_COST_PARAM, "0"));
		double switchABPCostFactor = Double.parseDouble(line.getOptionValue(SWITCH_ABP_COST_PARAM, "10"));
		int iniABP = Integer.parseInt(line.getOptionValue(INI_ABP_PARAM, "9"));
		int iniABCount = Integer.parseInt(line.getOptionValue(INI_AB_COUNT_PARAM, "0"));
		int iniNewClientCount = Integer.parseInt(line.getOptionValue(INI_NEW_CLIENT_COUNT_PARAM, "10"));
		double clientArrivalRate = Double.parseDouble(line.getOptionValue(CLIENT_ARRIVAL_RATE_PARAM, "10"));

		ClinicCostProfile clinicCostProfile = new ClinicCostProfile(revenuePerPatient, overtimeCostPerPatient,
				idleTimeCostPerPatient, leadTimeCostFactor, switchABPCostFactor);
		SchedulingContext schedulingContext = new SchedulingContext(capacity, maxABP, maxQueueSize, clinicCostProfile);

		ClinicSchedulingDemo demo = new ClinicSchedulingDemo(DEFAULT_BRANCH_FACTOR);
		demo.run(missionName, schedulingContext, iniABP, iniABCount, iniNewClientCount, clientArrivalRate);
	}

	private static Vocabulary getVocabulary(XMDP xmdp) {
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
