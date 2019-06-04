package mobilerobot.xplanning;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import examples.common.DSMException;
import examples.common.XPlanningOutDirectories;
import examples.mobilerobot.demo.MobileRobotDemo;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;

public class XPlanningRunner {

	private MobileRobotDemo mDemo;
	private XPlanningOutDirectories mOutputDirs;

	public XPlanningRunner(File mapsJsonDir, XPlanningOutDirectories outputDirs) {
		mDemo = new MobileRobotDemo(mapsJsonDir, outputDirs);
		mOutputDirs = outputDirs;
	}

	public void runMission(File missionJsonFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		mDemo.runXPlanning(missionJsonFile);
	}

	public void runAllMissions(File missionsJsonDir)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		if (!missionsJsonDir.isDirectory()) {
			mDemo.runXPlanning(missionsJsonDir);
		} else {
			for (File subDirOrFile : missionsJsonDir.listFiles()) {
				runAllMissions(subDirOrFile);
			}
		}
	}

	public XPlanningOutDirectories getXPlanningOutDirectories() {
		return mOutputDirs;
	}

	public static void main(String[] args) throws URISyntaxException, PrismException, IOException, XMDPException,
			PrismConnectorException, GRBException, DSMException {
		XPlanningOutDirectories outputDirs = FileIOUtils.createXPlanningOutDirectories();

		File mapsJsonDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);
		File missionsJsonRootDir;
		if (args.length > 0) {
			String missionsJsonRootPath = args[0];
			missionsJsonRootDir = new File(missionsJsonRootPath);
		} else {
			missionsJsonRootDir = FileIOUtils.getMissionsResourceDir(XPlanningRunner.class);
		}

		XPlanningRunner runner = new XPlanningRunner(mapsJsonDir, outputDirs);
		runner.runAllMissions(missionsJsonRootDir);
	}

}
