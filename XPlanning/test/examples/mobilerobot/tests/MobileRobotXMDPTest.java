package examples.mobilerobot.tests;

import java.io.File;
import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import language.exceptions.IncompatibleActionException;
import language.mdp.XMDP;

public class MobileRobotXMDPTest {

	private MobileRobotTestLoader mTestLoader;

	@Test(dataProvider = "xmdpProblems")
	public void testXMDPConstructor(File missionJsonFile, XMDP xmdp) {
		System.out.println("mission: " + missionJsonFile.getName());
	}

	@DataProvider(name = "xmdpProblems")
	public Object[][] xmdpProblems()
			throws IncompatibleActionException, IOException, ParseException, MapTopologyException {
		String mapJsonDirPath = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/maps";
		String missionJsonDirPath = "/Users/rsukkerd/Projects/explainable-planning/XPlanning/data/missions";

		mTestLoader = new MobileRobotTestLoader(mapJsonDirPath, missionJsonDirPath);
		File missionJsonDir = new File(missionJsonDirPath);
		File[] missionJsonFiles = missionJsonDir.listFiles();
		Object[][] data = new Object[missionJsonFiles.length][2];

		int i = 0;
		for (File missionJsonFile : missionJsonFiles) {
			XMDP xmdp = mTestLoader.loadXMDP(missionJsonFile);
			data[i] = new Object[] { missionJsonFile, xmdp };
		}
		return data;
	}

	@BeforeClass
	public void beforeClass() {
	}

}
