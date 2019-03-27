package examples.mobilerobot.demo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.parser.ParseException;

import examples.common.DSMException;
import examples.common.IXMDPLoader;
import examples.mobilerobot.dsm.IEdgeAttribute;
import examples.mobilerobot.dsm.INodeAttribute;
import examples.mobilerobot.dsm.LocationNode;
import examples.mobilerobot.dsm.MapTopology;
import examples.mobilerobot.dsm.Mission;
import examples.mobilerobot.dsm.MobileRobotXMDPBuilder;
import examples.mobilerobot.dsm.parser.AreaParser;
import examples.mobilerobot.dsm.parser.IEdgeAttributeParser;
import examples.mobilerobot.dsm.parser.INodeAttributeParser;
import examples.mobilerobot.dsm.parser.MapTopologyReader;
import examples.mobilerobot.dsm.parser.MissionReader;
import examples.mobilerobot.dsm.parser.OcclusionParser;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Occlusion;
import language.exceptions.XMDPException;
import language.mdp.XMDP;

public class MobileRobotXMDPLoader implements IXMDPLoader {

	private static final Area DEFAULT_AREA = Area.PUBLIC;
	private static final Occlusion DEFAULT_OCCLUSION = Occlusion.CLEAR;

	private File mMapsJsonDir;
	private MapTopologyReader mMapReader;
	private MissionReader mMissionReader = new MissionReader();
	private MobileRobotXMDPBuilder mXMDPBuilder = new MobileRobotXMDPBuilder();
	private Map<String, INodeAttribute> mDefaultNodeAttributes = new HashMap<>();
	private Map<String, IEdgeAttribute> mDefaultEdgeAttributes = new HashMap<>();

	public MobileRobotXMDPLoader(File mapsJsonDir) {
		mMapsJsonDir = mapsJsonDir;
		AreaParser areaParser = new AreaParser();
		OcclusionParser occlusionParser = new OcclusionParser();
		Set<INodeAttributeParser<? extends INodeAttribute>> nodeAttributeParsers = new HashSet<>();
		nodeAttributeParsers.add(areaParser);
		Set<IEdgeAttributeParser<? extends IEdgeAttribute>> edgeAttributeParsers = new HashSet<>();
		edgeAttributeParsers.add(occlusionParser);
		mMapReader = new MapTopologyReader(nodeAttributeParsers, edgeAttributeParsers);

		// Default node/edge attribute values
		mDefaultNodeAttributes.put(areaParser.getAttributeName(), DEFAULT_AREA);
		mDefaultEdgeAttributes.put(occlusionParser.getAttributeName(), DEFAULT_OCCLUSION);
	}

	@Override
	public XMDP loadXMDP(File missionJsonFile) throws DSMException, XMDPException {
		Mission mission;
		try {
			mission = mMissionReader.readMission(missionJsonFile);
		} catch (IOException | ParseException e) {
			throw new DSMException(e.getMessage());
		}
		String mapJsonFilename = mission.getMapJSONFilename();
		File mapJsonFile = new File(mMapsJsonDir, mapJsonFilename);
		MapTopology map;
		try {
			map = mMapReader.readMapTopology(mapJsonFile, mDefaultNodeAttributes, mDefaultEdgeAttributes);
		} catch (IOException | ParseException e) {
			throw new DSMException(e.getMessage());
		}
		LocationNode startNode = map.lookUpLocationNode(mission.getStartNodeID());
		LocationNode goalNode = map.lookUpLocationNode(mission.getGoalNodeID());
		return mXMDPBuilder.buildXMDP(map, startNode, goalNode, mission.getPreferenceInfo());
	}
}
