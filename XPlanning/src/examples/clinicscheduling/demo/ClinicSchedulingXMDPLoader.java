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

import examples.clinicscheduling.dsm.ClinicCostProfile;
import examples.clinicscheduling.dsm.ClinicSchedulingXMDPBuilder;
import examples.clinicscheduling.dsm.SchedulingContext;
import examples.common.DSMException;
import examples.common.IXMDPLoader;
import language.exceptions.XMDPException;
import language.mdp.XMDP;

public class ClinicSchedulingXMDPLoader implements IXMDPLoader {

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

	public ClinicSchedulingXMDPLoader(int branchFactor) {
		mXMDPBuilder = new ClinicSchedulingXMDPBuilder(branchFactor);
	}

	@Override
	public XMDP loadXMDP(File problemFile) throws XMDPException, DSMException {
		String argsLine = null;

		try (FileReader fileReader = new FileReader(problemFile);
				BufferedReader buffReader = new BufferedReader(fileReader);) {
			argsLine = buffReader.readLine();
		} catch (IOException e) {
			throw new DSMException(e.getMessage());
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

		CommandLine line = null;
		try {
			line = parser.parse(options, argsArray);
		} catch (ParseException e) {
			throw new DSMException(e.getMessage());
		}
		int capacity = Integer.parseInt(line.getOptionValue(CAPACITY_PARAM, "10"));
		int maxABP = Integer.parseInt(line.getOptionValue(MAX_ABP_PARAM, "10"));
		int maxQueueSize = Integer.parseInt(line.getOptionValue(MAX_QUEUE_SIZE_PARAM, "100"));
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

		return mXMDPBuilder.buildXMDP(schedulingContext, iniABP, iniABCount, iniNewClientCount, clientArrivalRate);
	}
}
