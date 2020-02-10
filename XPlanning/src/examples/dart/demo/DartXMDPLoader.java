package examples.dart.demo;

import java.io.File;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.dart.dsm.DartMission;
import examples.dart.dsm.DartMissionReader;
import examples.dart.dsm.DartXMDPBuilder;
import language.exceptions.XMDPException;
import language.mdp.XMDP;

public class DartXMDPLoader implements IXMDPLoader {

	private DartXMDPBuilder mXMDPBuilder = new DartXMDPBuilder();
	private DartMissionReader mMissionReader = new DartMissionReader();

	public DartXMDPLoader() {
		// This constructor can take any DART domain configuration parameters as arguments
	}

	@Override
	public XMDP loadXMDP(File problemFile) throws DSMException, XMDPException {
		DartMission mission = mMissionReader.readDartMission(problemFile);
		return mXMDPBuilder.buildXMDP(mission);
	}

}
