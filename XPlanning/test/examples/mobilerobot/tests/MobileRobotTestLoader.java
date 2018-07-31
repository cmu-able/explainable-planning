package examples.mobilerobot.tests;

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
import language.exceptions.IncompatibleActionException;
import language.mdp.XMDP;

public class MobileRobotTestLoader {

	private MapTopologyReader mMapReader;
	private MissionReader mMissionReader = new MissionReader();
	private MobileRobotXMDPBuilder mXMDPBuilder = new MobileRobotXMDPBuilder();

	public MobileRobotTestLoader() {
		AreaParser areaParser = new AreaParser();
		OcclusionParser occlusionParser = new OcclusionParser();
		Set<INodeAttributeParser<? extends INodeAttribute>> nodeAttributeParsers = new HashSet<>();
		nodeAttributeParsers.add(areaParser);
		Set<IEdgeAttributeParser<? extends IEdgeAttribute>> edgeAttributeParsers = new HashSet<>();
		edgeAttributeParsers.add(occlusionParser);
		mMapReader = new MapTopologyReader(nodeAttributeParsers, edgeAttributeParsers);
	}

	public XMDP loadXMDP(String mapJsonDir, String mapJsonFilename, String missionJsonDir, String missionJsonFilename)
			throws IOException, ParseException, MapTopologyException, IncompatibleActionException {
		MapTopology map = mMapReader.readMapTopology(mapJsonDir, mapJsonFilename);
		Mission mission = mMissionReader.readMission(missionJsonDir, missionJsonFilename);
		LocationNode startNode = map.lookUpLocationNode(mission.getStartNodeID());
		LocationNode goalNode = map.lookUpLocationNode(mission.getGoalNodeID());
		return mXMDPBuilder.buildXMDP(map, startNode, goalNode, mission.getMaxTravelTime());
	}
}
