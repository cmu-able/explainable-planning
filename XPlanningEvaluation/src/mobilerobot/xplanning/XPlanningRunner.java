package mobilerobot.xplanning;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import examples.common.DSMException;
import examples.common.Directories;
import examples.mobilerobot.demo.MobileRobotDemo;
import gurobi.GRBException;
import language.exceptions.XMDPException;
import mobilerobot.missiongen.MissionJSONGenerator;
import mobilerobot.utilities.FileIOUtils;
import prism.PrismException;
import solver.prismconnector.exceptions.PrismConnectorException;

public class XPlanningRunner {

	private MobileRobotDemo mDemo;
	private File mMissionsJsonRootDir;

	public XPlanningRunner(File mapsJsonDir, File missionsJsonRootDir, Directories outputDirs) {
		mDemo = new MobileRobotDemo(mapsJsonDir, outputDirs);
		mMissionsJsonRootDir = missionsJsonRootDir;
	}

	public void runAllMissions()
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		FileFilter dirFilter = File::isDirectory;
		for (File missionsJsonSubDir : mMissionsJsonRootDir.listFiles(dirFilter)) {
			for (File missionJsonFile : missionsJsonSubDir.listFiles()) {
				mDemo.run(missionJsonFile);
			}
		}
	}

	public static void main(String[] args) throws URISyntaxException, PrismException, IOException, XMDPException,
			PrismConnectorException, GRBException, DSMException {
		Path policiesOutputPath = null;
		Path explanationOutputPath = null;
		Path prismOutputPath = null;
		Directories outputDirs = new Directories(policiesOutputPath, explanationOutputPath, prismOutputPath);

		File mapsJsonDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);
		File missionsJsonRootDir = FileIOUtils.getMissionsResourceDir(XPlanningRunner.class);
		XPlanningRunner runner = new XPlanningRunner(mapsJsonDir, missionsJsonRootDir, outputDirs);
		runner.runAllMissions();
	}

}
