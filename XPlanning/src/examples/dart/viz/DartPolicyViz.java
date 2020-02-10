package examples.dart.viz;

import java.io.File;

import examples.common.DSMException;
import examples.dart.dsm.DartMission;
import examples.dart.dsm.DartMissionReader;
import language.policy.Policy;

public class DartPolicyViz {

	private DartMissionReader mMissionReader = new DartMissionReader();
	private DartMission mMission;

	public DartPolicyViz(File missionFile) throws DSMException {
		mMission = mMissionReader.readDartMission(missionFile);
	}

	public void visualizePolicy(Policy policy) {
		// TODO
	}
}
