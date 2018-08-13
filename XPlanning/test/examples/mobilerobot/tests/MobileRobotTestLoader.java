package examples.mobilerobot.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.json.simple.parser.ParseException;

import examples.mobilerobot.dsm.IEdgeAttribute;
import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.Mission;
import examples.mobilerobot.dsm.MobileRobotXMDPBuilder;
import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.dsm.parser.AreaParser;
import examples.mobilerobot.dsm.parser.IEdgeAttributeParser;
import examples.mobilerobot.dsm.parser.INodeAttributeParser;
import examples.mobilerobot.dsm.parser.MapTopologyReader;
import examples.mobilerobot.dsm.parser.MissionReader;
import examples.mobilerobot.dsm.parser.OcclusionParser;
import language.exceptions.XMDPException;
import language.mdp.XMDP;

public class MobileRobotTestLoader {

	private MapTopologyReader mMapReader;
	private MissionReader mMissionReader = new MissionReader();
	private MobileRobotXMDPBuilder mXMDPBuilder = new MobileRobotXMDPBuilder();
	private File mMapJsonDir;
	private File mMissionJsonDir;

	public MobileRobotTestLoader(String mapJsonDirPath, String missionJsonDirPath) {
		mMapJsonDir = new File(mapJsonDirPath);
		mMissionJsonDir = new File(missionJsonDirPath);
		AreaParser areaParser = new AreaParser();
		OcclusionParser occlusionParser = new OcclusionParser();
		Set<INodeAttributeParser<? extends INodeAttribute>> nodeAttributeParsers = new HashSet<>();
		nodeAttributeParsers.add(areaParser);
		Set<IEdgeAttributeParser<? extends IEdgeAttribute>> edgeAttributeParsers = new HashSet<>();
		edgeAttributeParsers.add(occlusionParser);
		mMapReader = new MapTopologyReader(nodeAttributeParsers, edgeAttributeParsers);
	}

	public Set<XMDP> loadAllXMDPs() throws IOException, ParseException, MapTopologyException, XMDPException {
		Set<XMDP> xmdps = new HashSet<>();
		File[] missionJsonFiles = mMissionJsonDir.listFiles();
		for (File missionJsonFile : missionJsonFiles) {
			xmdps.add(loadXMDP(missionJsonFile));
		}
		return xmdps;
	}

	public XMDP loadXMDP(File missionJsonFile) throws IOException, ParseException, MapTopologyException, XMDPException {
		Mission mission = mMissionReader.readMission(missionJsonFile);
		String mapJsonFilename = mission.getMapJSONFilename();
		File mapJsonFile = new File(mMapJsonDir, mapJsonFilename);
		MapTopology map = mMapReader.readMapTopology(mapJsonFile);
		LocationNode startNode = map.lookUpLocationNode(mission.getStartNodeID());
		LocationNode goalNode = map.lookUpLocationNode(mission.getGoalNodeID());
		return mXMDPBuilder.buildXMDP(map, startNode, goalNode, mission.getMaxTravelTime());
	}
}
