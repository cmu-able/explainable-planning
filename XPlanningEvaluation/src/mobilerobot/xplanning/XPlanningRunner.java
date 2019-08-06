package mobilerobot.xplanning;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import examples.common.DSMException;
import examples.common.XPlannerOutDirectories;
import examples.mobilerobot.demo.MobileRobotXPlanner;
import explanation.verbalization.VerbalizerSettings;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;

public class XPlanningRunner {

	private MobileRobotXPlanner mDemo;
	private XPlannerOutDirectories mOutputDirs;

	public XPlanningRunner(File mapsJsonDir, XPlannerOutDirectories outputDirs, VerbalizerSettings verbalizerSettings) {
		mDemo = new MobileRobotXPlanner(mapsJsonDir, outputDirs, verbalizerSettings);
		mOutputDirs = outputDirs;
	}

	public void runMission(File missionJsonFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		mDemo.runXPlanning(missionJsonFile);
	}

	public void runAllMissions(File missionsJsonDir)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		if (!missionsJsonDir.isDirectory()) {
			runMission(missionsJsonDir);
		} else {
			for (File subDirOrFile : missionsJsonDir.listFiles()) {
				runAllMissions(subDirOrFile);
			}
		}
	}

	public XPlannerOutDirectories getXPlannerOutDirectories() {
		return mOutputDirs;
	}

	public static void main(String[] args) throws URISyntaxException, PrismException, IOException, XMDPException,
			PrismConnectorException, GRBException, DSMException {
		XPlannerOutDirectories outputDirs = FileIOUtils.createXPlannerOutDirectories();

		File mapsJsonDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);
		File missionsJsonRootDir;
		if (args.length > 0) {
			String missionsJsonRootPath = args[0];
			missionsJsonRootDir = new File(missionsJsonRootPath);
		} else {
			missionsJsonRootDir = FileIOUtils.getMissionsResourceDir(XPlanningRunner.class);
		}

		VerbalizerSettings verbalizerSettings = new VerbalizerSettings(); // describe costs
		verbalizerSettings.setQADecimalFormatter(MobileRobotXPlanner.getQADecimalFormatter());
		MobileRobotXPlanner.setVerbalizerOrdering(verbalizerSettings);

		XPlanningRunner runner = new XPlanningRunner(mapsJsonDir, outputDirs, verbalizerSettings);
		runner.runAllMissions(missionsJsonRootDir);
	}

}
