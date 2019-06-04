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

	public XPlanningRunner(File mapsJsonDir, XPlanningOutDirectories outputDirs) {
		mDemo = new MobileRobotDemo(mapsJsonDir, outputDirs);
	}

	public void runAllMissions(File missionDirOrFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		if (!missionDirOrFile.isDirectory()) {
			mDemo.runXPlanning(missionDirOrFile);
		} else {
			for (File subDirOrFile : missionDirOrFile.listFiles()) {
				runAllMissions(subDirOrFile);
			}
		}
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
