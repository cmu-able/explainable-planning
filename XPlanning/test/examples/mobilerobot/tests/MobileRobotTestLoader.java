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
import examples.mobilerobot.metrics.CollisionDomain;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.IntrusivenessDomain;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.qfactors.MoveToAction;
import explanation.verbalization.Vocabulary;
import language.exceptions.IncompatibleActionException;
import language.mdp.QSpace;
import language.mdp.XMDP;
import language.metrics.CountQFunction;
import language.metrics.NonStandardMetricQFunction;

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

	public Set<XMDP> loadAllXMDPs()
			throws IncompatibleActionException, IOException, ParseException, MapTopologyException {
		Set<XMDP> xmdps = new HashSet<>();
		File[] missionJsonFiles = mMissionJsonDir.listFiles();
		for (File missionJsonFile : missionJsonFiles) {
			xmdps.add(loadXMDP(missionJsonFile));
		}
		return xmdps;
	}

	public XMDP loadXMDP(File missionJsonFile)
			throws IOException, ParseException, MapTopologyException, IncompatibleActionException {
		Mission mission = mMissionReader.readMission(missionJsonFile);
		String mapJsonFilename = mission.getMapJSONFilename();
		File mapJsonFile = new File(mMapJsonDir, mapJsonFilename);
		MapTopology map = mMapReader.readMapTopology(mapJsonFile);
		LocationNode startNode = map.lookUpLocationNode(mission.getStartNodeID());
		LocationNode goalNode = map.lookUpLocationNode(mission.getGoalNodeID());
		return mXMDPBuilder.buildXMDP(map, startNode, goalNode, mission.getMaxTravelTime());
	}

	public Vocabulary getVocabulary(XMDP xmdp) {
		QSpace qSpace = xmdp.getQSpace();
		TravelTimeQFunction timeQFunction = qSpace.getQFunction(TravelTimeQFunction.class, TravelTimeQFunction.NAME);
		CountQFunction<MoveToAction, CollisionDomain, CollisionEvent> collideQFunction = qSpace
				.getQFunction(CountQFunction.class, CollisionEvent.NAME);
		NonStandardMetricQFunction<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> intrusiveQFunction = qSpace
				.getQFunction(NonStandardMetricQFunction.class, IntrusiveMoveEvent.NAME);

		Vocabulary vocab = new Vocabulary();
		vocab.putNoun(timeQFunction, "time");
		vocab.putVerb(timeQFunction, "take");
		vocab.putUnit(timeQFunction, "minute", "minutes");
		vocab.putNoun(collideQFunction, "collision");
		vocab.putVerb(collideQFunction, "have");
		vocab.putUnit(collideQFunction, "collision", "collisions");
		vocab.putNoun(intrusiveQFunction, "intrusiveness");
		vocab.putVerb(intrusiveQFunction, "be");
		for (IntrusiveMoveEvent event : intrusiveQFunction.getEventBasedMetric().getEvents()) {
			vocab.putCategoricalValue(intrusiveQFunction, event, event.getName());
		}
		vocab.putUnit(intrusiveQFunction, "step", "steps");
		return vocab;
	}
}
