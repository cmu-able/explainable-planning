package mobilerobot.xplanning;

import java.io.File;
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
		runAllMissions(mMissionsJsonRootDir);
	}

	private void runAllMissions(File dirOrFile)
			throws PrismException, IOException, XMDPException, PrismConnectorException, GRBException, DSMException {
		if (!dirOrFile.isDirectory()) {
			mDemo.run(dirOrFile);
		} else {
			for (File subDirOrFile : dirOrFile.listFiles()) {
				runAllMissions(subDirOrFile);
			}
		}
	}

	public static void main(String[] args) throws URISyntaxException, PrismException, IOException, XMDPException,
			PrismConnectorException, GRBException, DSMException {
		Path outputPath = FileIOUtils.getOutputDir().toPath();
		Path policiesOutputPath = outputPath.resolve("policies");
		Path explanationsOutputPath = outputPath.resolve("explanations");
		Path prismOutputPath = outputPath.resolve("prism");
		Directories outputDirs = new Directories(policiesOutputPath, explanationsOutputPath, prismOutputPath);

		File mapsJsonDir = FileIOUtils.getMapsResourceDir(MissionJSONGenerator.class);
		File missionsJsonRootDir = FileIOUtils.getMissionsResourceDir(XPlanningRunner.class);
		XPlanningRunner runner = new XPlanningRunner(mapsJsonDir, missionsJsonRootDir, outputDirs);
		runner.runAllMissions();
	}

}
