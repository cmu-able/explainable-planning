package examples.dart.demo;

import java.io.File;

import org.apache.commons.cli.Options;

import examples.common.CommandLineXMDPLoader;
import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.dart.dsm.DartMission;
import examples.dart.dsm.DartXMDPBuilder;
import examples.dart.dsm.TeamConfiguration;
import language.exceptions.XMDPException;
import language.mdp.XMDP;

public class DartXMDPLoader implements IXMDPLoader {

	private static final String MAX_ALT_PARAM = "maxAltitude";
	private static final String HORIZON_PARAM = "horizon";
	private static final String TARGET_READINGS_PARAM = "targetSensorReadings";
	private static final String THREAT_READINGS_PARAM = "threatSensorReadings";
	private static final String TARGET_WEIGHT_PARAM = "targetWeight";
	private static final String THREAT_WEIGHT_PARAM = "threatWeight";
	private static final String INI_ALT_PARAM = "iniAltitude";
	private static final String INI_FORM_PARAM = "iniFormation";
	private static final String INI_ECM_PARAM = "iniECM";

	private DartXMDPBuilder mXMDPBuilder = new DartXMDPBuilder();
	private CommandLineXMDPLoader mCLLoader;

	public DartXMDPLoader() {
		Options options = new Options();
		options.addOption("A", MAX_ALT_PARAM, true, "Maximum altitude level");
		options.addOption("H", HORIZON_PARAM, true, "Look-ahead horizon");
		options.addOption("G", TARGET_READINGS_PARAM, true, "Target sensor readings");
		options.addOption("D", THREAT_READINGS_PARAM, true, "Threat sensor readings");
		options.addOption("g", TARGET_WEIGHT_PARAM, true, "Target weight");
		options.addOption("d", THREAT_WEIGHT_PARAM, true, "Threat weight");
		options.addOption("a", INI_ALT_PARAM, true, "Team's initial altitude level");
		options.addOption("f", INI_FORM_PARAM, true, "Team's initial formation");
		options.addOption("e", INI_ECM_PARAM, true, "Team's initial ECM state");

		mCLLoader = new CommandLineXMDPLoader(options);
	}

	@Override
	public XMDP loadXMDP(File problemFile) throws DSMException, XMDPException {
		mCLLoader.loadCommandLineFromFile(problemFile);

		int altitude = mCLLoader.getIntArgument(INI_ALT_PARAM);
		String formation = mCLLoader.getStringArgument(INI_FORM_PARAM);
		boolean ecm = mCLLoader.getBooleanArgument(INI_ECM_PARAM);
		int maxAltLevel = mCLLoader.getIntArgument(MAX_ALT_PARAM);
		int horizon = mCLLoader.getIntArgument(HORIZON_PARAM);
		double[] expTargetProbs = mCLLoader.getDoubleArrayArgument(TARGET_READINGS_PARAM);
		double[] expThreatProbs = mCLLoader.getDoubleArrayArgument(THREAT_READINGS_PARAM);
		double targetWeight = mCLLoader.getDoubleArgument(TARGET_WEIGHT_PARAM);
		double threatWeight = mCLLoader.getDoubleArgument(THREAT_WEIGHT_PARAM);

		TeamConfiguration iniTeamConfig = new TeamConfiguration(altitude, formation, ecm);
		DartMission mission = new DartMission(iniTeamConfig, maxAltLevel, horizon, expTargetProbs, expThreatProbs,
				targetWeight, threatWeight);
		return mXMDPBuilder.buildXMDP(mission);
	}

}
