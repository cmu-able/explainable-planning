package examples.mobilerobot.dsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.dsm.exceptions.NodeAttributeNotFoundException;
import examples.mobilerobot.metrics.CollisionDomain;
import examples.mobilerobot.metrics.CollisionEvent;
import examples.mobilerobot.metrics.IntrusiveMoveEvent;
import examples.mobilerobot.metrics.IntrusivenessDomain;
import examples.mobilerobot.metrics.TravelTimeDomain;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import examples.mobilerobot.models.Area;
import examples.mobilerobot.models.Distance;
import examples.mobilerobot.models.Location;
import examples.mobilerobot.models.MoveToAction;
import examples.mobilerobot.models.Occlusion;
import examples.mobilerobot.models.RobotLocationActionDescription;
import examples.mobilerobot.models.RobotSpeed;
import examples.mobilerobot.models.RobotSpeedActionDescription;
import examples.mobilerobot.models.SetSpeedAction;
import language.domain.metrics.CountQFunction;
import language.domain.metrics.EventBasedMetric;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.NonStandardMetricQFunction;
import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.ActionSpace;
import language.mdp.FactoredPSO;
import language.mdp.Precondition;
import language.mdp.QSpace;
import language.mdp.StateSpace;
import language.mdp.StateVarTuple;
import language.mdp.TransitionFunction;
import language.mdp.XMDP;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import solver.common.Constants;

public class MobileRobotXMDPBuilder {

	// Robot's default setting
	private static final RobotSpeed DEFAULT_SPEED = new RobotSpeed(0.35);

	// --- Location --- //
	// Robot's location state variable
	private StateVarDefinition<Location> rLocDef;

	// Move actions (depends on map topology)

	// MoveTo action definition
	private ActionDefinition<MoveToAction> moveToDef;
	// ------ //

	// --- Speed setting --- //
	public static final double HALF_SPEED = 0.35;
	public static final double FULL_SPEED = 0.7;

	// Speed settings (known, fixed)
	private RobotSpeed halfSpeed = new RobotSpeed(HALF_SPEED);
	private RobotSpeed fullSpeed = new RobotSpeed(FULL_SPEED);

	// Robot's speed state variable
	private StateVarDefinition<RobotSpeed> rSpeedDef = new StateVarDefinition<>("rSpeed", halfSpeed, fullSpeed);

	// Speed-setting actions
	private SetSpeedAction setSpeedHalf = new SetSpeedAction(rSpeedDef.getStateVar(halfSpeed));
	private SetSpeedAction setSpeedFull = new SetSpeedAction(rSpeedDef.getStateVar(fullSpeed));

	// SetSpeed action definition
	private ActionDefinition<SetSpeedAction> setSpeedDef = new ActionDefinition<>("setSpeed", setSpeedHalf,
			setSpeedFull);
	// ------ //

	// --- QA functions --- //

	// --- Travel time --- //

	// --- Collision --- //
	private static final double SAFE_SPEED = 0.6;

	// --- Intrusiveness --- //
	public static final double NON_INTRUSIVE_PENALTY = 0;
	public static final double SEMI_INTRUSIVE_PEANLTY = 1;
	public static final double VERY_INTRUSIVE_PENALTY = 3;

	// ------ //

	// Map location nodes to the corresponding location values
	// To be used when adding derived attribute values to move-actions
	private Map<LocationNode, Location> mLocMap = new HashMap<>();

	public MobileRobotXMDPBuilder() {
		// Constructor may take as input other DSMs
	}

	public XMDP buildXMDP(MapTopology map, LocationNode startNode, LocationNode goalNode, PreferenceInfo prefInfo)
			throws XMDPException, MapTopologyException {
		StateSpace stateSpace = buildStateSpace(map);
		ActionSpace actionSpace = buildActionSpace(map);
		StateVarTuple initialState = buildInitialState(startNode);
		StateVarTuple goal = buildGoal(goalNode);
		TransitionFunction transFunction = buildTransitionFunction(map);
		QSpace qSpace = buildQFunctions();
		CostFunction costFunction = buildCostFunction(qSpace, prefInfo);
		return new XMDP(stateSpace, actionSpace, initialState, goal, transFunction, qSpace, costFunction);
	}

	private StateSpace buildStateSpace(MapTopology map) throws NodeAttributeNotFoundException {
		Set<Location> locs = new HashSet<>();
		for (LocationNode node : map) {
			Area area = node.getNodeAttribute(Area.class, "area");
			Location loc = new Location(node.getNodeID(), area);
			locs.add(loc);

			// Map each location node to its corresponding location value
			mLocMap.put(node, loc);
		}

		rLocDef = new StateVarDefinition<>("rLoc", locs);

		StateSpace stateSpace = new StateSpace();
		stateSpace.addStateVarDefinition(rLocDef);
		stateSpace.addStateVarDefinition(rSpeedDef);
		return stateSpace;
	}

	private ActionSpace buildActionSpace(MapTopology map) throws MapTopologyException {
		// MoveTo actions
		Set<MoveToAction> moveTos = new HashSet<>();

		// Assume that all locations are reachable
		for (Location locDest : rLocDef.getPossibleValues()) {
			MoveToAction moveTo = new MoveToAction(rLocDef.getStateVar(locDest));

			// Derived attributes for each move action are obtained from edges in the map
			LocationNode node = map.lookUpLocationNode(locDest.getId());
			Set<Connection> connections = map.getConnections(node);
			for (Connection conn : connections) {
				Location locSrc = mLocMap.get(conn.getOtherNode(node));

				// Distance
				Distance distance = new Distance(conn.getDistance());
				moveTo.putDistanceValue(distance, rLocDef.getStateVar(locSrc));

				// Occlusion
				Occlusion occlusion = conn.getConnectionAttribute(Occlusion.class, "occlusion");
				moveTo.putOcclusionValue(occlusion, rLocDef.getStateVar(locSrc));
			}

			moveTos.add(moveTo);
		}

		// MoveTo action definition
		moveToDef = new ActionDefinition<>("moveTo", moveTos);

		ActionSpace actionSpace = new ActionSpace();
		actionSpace.addActionDefinition(moveToDef);
		actionSpace.addActionDefinition(setSpeedDef);
		return actionSpace;
	}

	private StateVarTuple buildInitialState(LocationNode startNode) {
		Location loc = mLocMap.get(startNode);
		StateVarTuple initialState = new StateVarTuple();
		initialState.addStateVar(rLocDef.getStateVar(loc));
		initialState.addStateVar(rSpeedDef.getStateVar(DEFAULT_SPEED));
		return initialState;
	}

	private StateVarTuple buildGoal(LocationNode goalNode) {
		Location loc = mLocMap.get(goalNode);
		StateVarTuple goal = new StateVarTuple();
		goal.addStateVar(rLocDef.getStateVar(loc));
		return goal;
	}

	private TransitionFunction buildTransitionFunction(MapTopology map) throws XMDPException, MapTopologyException {
		// MoveTo:
		// Precondition
		Precondition<MoveToAction> preMoveTo = new Precondition<>(moveToDef);

		for (MoveToAction moveTo : moveToDef.getActions()) {
			Location locDest = moveTo.getDestination();

			// Source location for each move action from the map
			LocationNode node = map.lookUpLocationNode(locDest.getId());
			Set<Connection> connections = map.getConnections(node);
			for (Connection conn : connections) {
				Location locSrc = mLocMap.get(conn.getOtherNode(node));
				preMoveTo.add(moveTo, rLocDef, locSrc);
			}
		}

		// Action description for rLoc
		RobotLocationActionDescription rLocActionDesc = new RobotLocationActionDescription(moveToDef, preMoveTo,
				rLocDef);

		// PSO
		FactoredPSO<MoveToAction> moveToPSO = new FactoredPSO<>(moveToDef, preMoveTo);
		moveToPSO.addActionDescription(rLocActionDesc);

		// SetSpeed:
		// Precondition
		Precondition<SetSpeedAction> preSetSpeed = new Precondition<>(setSpeedDef);
		preSetSpeed.add(setSpeedHalf, rSpeedDef, fullSpeed);
		preSetSpeed.add(setSpeedFull, rSpeedDef, halfSpeed);

		// Action description for rSpeed
		RobotSpeedActionDescription rSpeedActionDesc = new RobotSpeedActionDescription(setSpeedDef, preSetSpeed,
				rSpeedDef);

		// PSO
		FactoredPSO<SetSpeedAction> setSpeedPSO = new FactoredPSO<>(setSpeedDef, preSetSpeed);
		setSpeedPSO.addActionDescription(rSpeedActionDesc);

		TransitionFunction transFunction = new TransitionFunction();
		transFunction.add(moveToPSO);
		transFunction.add(setSpeedPSO);
		return transFunction;
	}

	private QSpace buildQFunctions() {
		// Travel time
		TravelTimeDomain timeDomain = new TravelTimeDomain(rLocDef, rSpeedDef, moveToDef, rLocDef);
		TravelTimeQFunction timeQFunction = new TravelTimeQFunction(timeDomain);

		// Collision
		CollisionDomain collDomain = new CollisionDomain(rLocDef, rSpeedDef, moveToDef);
		CollisionEvent collEvent = new CollisionEvent(collDomain, SAFE_SPEED);
		CountQFunction<MoveToAction, CollisionDomain, CollisionEvent> collisionQFunction = new CountQFunction<>(
				collEvent);

		// Intrusiveness
		IntrusivenessDomain intrusiveDomain = new IntrusivenessDomain(moveToDef, rLocDef);
		IntrusiveMoveEvent nonIntrusive = new IntrusiveMoveEvent(IntrusiveMoveEvent.NON_INTRUSIVE_EVENT_NAME,
				intrusiveDomain, Area.PUBLIC);
		IntrusiveMoveEvent somewhatIntrusive = new IntrusiveMoveEvent(IntrusiveMoveEvent.SOMEWHAT_INTRUSIVE_EVENT_NAME,
				intrusiveDomain, Area.SEMI_PRIVATE);
		IntrusiveMoveEvent veryIntrusive = new IntrusiveMoveEvent(IntrusiveMoveEvent.VERY_INTRUSIVE_EVENT_NAME,
				intrusiveDomain, Area.PRIVATE);
		EventBasedMetric<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> metric = new EventBasedMetric<>(
				IntrusiveMoveEvent.NAME, intrusiveDomain);
		metric.putEventValue(nonIntrusive, NON_INTRUSIVE_PENALTY);
		metric.putEventValue(somewhatIntrusive, SEMI_INTRUSIVE_PEANLTY);
		metric.putEventValue(veryIntrusive, VERY_INTRUSIVE_PENALTY);
		NonStandardMetricQFunction<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> intrusiveQFunction = new NonStandardMetricQFunction<>(
				metric);

		QSpace qSpace = new QSpace();
		qSpace.addQFunction(timeQFunction);
		qSpace.addQFunction(collisionQFunction);
		qSpace.addQFunction(intrusiveQFunction);
		return qSpace;
	}

	private CostFunction buildCostFunction(QSpace qSpace, PreferenceInfo prefInfo) {
		CostFunction costFunction = new CostFunction(Constants.SSP_COST_OFFSET);
		for (IQFunction<?, ?> qFunction : qSpace) {
			addAttributeCostFunctions(qFunction, prefInfo, costFunction);
		}
		return costFunction;
	}

	private <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> void addAttributeCostFunctions(
			S qFunction, PreferenceInfo prefInfo, CostFunction costFunction) {
		double aConst = prefInfo.getaConst(qFunction.getName());
		double bConst = prefInfo.getbConst(qFunction.getName());
		AttributeCostFunction<S> attrCostFunction = new AttributeCostFunction<>(qFunction, aConst, bConst);
		costFunction.put(attrCostFunction, prefInfo.getScalingConst(qFunction.getName()));
	}
}
