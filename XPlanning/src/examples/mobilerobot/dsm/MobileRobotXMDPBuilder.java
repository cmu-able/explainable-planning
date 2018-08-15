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
import examples.mobilerobot.qfactors.Area;
import examples.mobilerobot.qfactors.Distance;
import examples.mobilerobot.qfactors.Location;
import examples.mobilerobot.qfactors.MoveToAction;
import examples.mobilerobot.qfactors.Occlusion;
import examples.mobilerobot.qfactors.RobotBumped;
import examples.mobilerobot.qfactors.RobotBumpedActionDescription;
import examples.mobilerobot.qfactors.RobotLocationActionDescription;
import examples.mobilerobot.qfactors.RobotSpeed;
import examples.mobilerobot.qfactors.RobotSpeedActionDescription;
import examples.mobilerobot.qfactors.SetSpeedAction;
import language.exceptions.XMDPException;
import language.mdp.ActionSpace;
import language.mdp.FactoredPSO;
import language.mdp.Precondition;
import language.mdp.QSpace;
import language.mdp.StateSpace;
import language.mdp.StateVarTuple;
import language.mdp.TransitionFunction;
import language.mdp.XMDP;
import language.metrics.CountQFunction;
import language.metrics.EventBasedMetric;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;
import language.metrics.NonStandardMetricQFunction;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import language.qfactors.ActionDefinition;
import language.qfactors.IAction;
import language.qfactors.StateVarDefinition;

public class MobileRobotXMDPBuilder {

	// Robot's default setting
	private static final RobotSpeed DEFAULT_SPEED = new RobotSpeed(0.35);

	// Robot's initial bump-sensor value
	private static final RobotBumped DEFAULT_BUMPED = new RobotBumped(false);

	// --- Location --- //
	// Robot's location state variable
	private StateVarDefinition<Location> rLocDef;

	// Move actions (depends on map topology)

	// MoveTo action definition
	private ActionDefinition<MoveToAction> moveToDef;
	// ------ //

	// --- Speed setting --- //
	// Speed settings (known, fixed)
	private RobotSpeed halfSpeed = new RobotSpeed(0.35);
	private RobotSpeed fullSpeed = new RobotSpeed(0.68);

	// Robot's speed state variable
	private StateVarDefinition<RobotSpeed> rSpeedDef = new StateVarDefinition<>("rSpeed", halfSpeed, fullSpeed);

	// Speed-setting actions
	private SetSpeedAction setSpeedHalf = new SetSpeedAction(rSpeedDef.getStateVar(halfSpeed));
	private SetSpeedAction setSpeedFull = new SetSpeedAction(rSpeedDef.getStateVar(fullSpeed));

	// SetSpeed action definition
	private ActionDefinition<SetSpeedAction> setSpeedDef = new ActionDefinition<>("setSpeed", setSpeedHalf,
			setSpeedFull);
	// ------ //

	// --- Bump sensor --- //
	// Bump sensor values (known, fixed)
	private RobotBumped bumped = new RobotBumped(true);
	private RobotBumped notBumped = new RobotBumped(false);

	// Robot's bump sensor state variable
	private StateVarDefinition<RobotBumped> rBumpedDef = new StateVarDefinition<>("rBumped", bumped, notBumped);
	// ------ //

	// --- QA functions --- //

	// --- Travel time --- //
	private TravelTimeQFunction timeQFunction;

	// --- Collision --- //
	private CountQFunction<MoveToAction, CollisionDomain, CollisionEvent> collisionQFunction;
	private static final double SAFE_SPEED = 0.6;

	// --- Intrusiveness --- //
	private NonStandardMetricQFunction<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> intrusiveQFunction;
	private static final double NON_INTRUSIVE_PENALTY = 0;
	private static final double SEMI_INTRUSIVE_PEANLTY = 1;
	private static final double VERY_INTRUSIVE_PENALTY = 3;

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
		CostFunction costFunction = buildCostFunction(prefInfo);
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
		stateSpace.addStateVarDefinition(rBumpedDef);
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
		initialState.addStateVar(rBumpedDef.getStateVar(DEFAULT_BUMPED));
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

		// Action description for rBumped
		RobotBumpedActionDescription rBumpedActionDesc = new RobotBumpedActionDescription(moveToDef, preMoveTo, rLocDef,
				rBumpedDef);

		// PSO
		FactoredPSO<MoveToAction> moveToPSO = new FactoredPSO<>(moveToDef, preMoveTo);
		moveToPSO.addActionDescription(rLocActionDesc);
		moveToPSO.addActionDescription(rBumpedActionDesc);

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
		timeQFunction = new TravelTimeQFunction(timeDomain);

		// Collision
		CollisionDomain collDomain = new CollisionDomain(rSpeedDef, moveToDef, rBumpedDef);
		CollisionEvent collEvent = new CollisionEvent(collDomain, SAFE_SPEED);
		collisionQFunction = new CountQFunction<>(collEvent);

		// Intrusiveness
		IntrusivenessDomain intrusiveDomain = new IntrusivenessDomain(moveToDef, rLocDef);
		IntrusiveMoveEvent nonIntrusive = new IntrusiveMoveEvent("non-intrusive", intrusiveDomain, Area.PUBLIC);
		IntrusiveMoveEvent somewhatIntrusive = new IntrusiveMoveEvent("somewhat-intrusive", intrusiveDomain,
				Area.SEMI_PRIVATE);
		IntrusiveMoveEvent veryIntrusive = new IntrusiveMoveEvent("very-intrusive", intrusiveDomain, Area.PRIVATE);
		EventBasedMetric<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> metric = new EventBasedMetric<>(
				IntrusiveMoveEvent.NAME, intrusiveDomain);
		metric.put(nonIntrusive, NON_INTRUSIVE_PENALTY);
		metric.put(somewhatIntrusive, SEMI_INTRUSIVE_PEANLTY);
		metric.put(veryIntrusive, VERY_INTRUSIVE_PENALTY);
		intrusiveQFunction = new NonStandardMetricQFunction<>(metric);

		QSpace qSpace = new QSpace();
		qSpace.addQFunction(timeQFunction);
		qSpace.addQFunction(collisionQFunction);
		qSpace.addQFunction(intrusiveQFunction);
		return qSpace;
	}

	private CostFunction buildCostFunction(PreferenceInfo prefInfo) {
		CostFunction costFunction = new CostFunction();
		IQFunction<?, ?>[] qFunctions = { timeQFunction, collisionQFunction, intrusiveQFunction };
		for (IQFunction<?, ?> qFunction : qFunctions) {
			addAttributeCostFunctions(qFunction, prefInfo, costFunction);
		}
		return costFunction;
	}

	private <E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> void addAttributeCostFunctions(
			S qFunction, PreferenceInfo prefInfo, CostFunction costFunction) {
		double minValue = prefInfo.getMinQAValue(qFunction.getName());
		double maxValue = prefInfo.getMaxQAValue(qFunction.getName());
		double aConst = minValue / (maxValue - minValue);
		double bConst = 1 / (maxValue - minValue);
		AttributeCostFunction<S> attrCostFunction = new AttributeCostFunction<>(qFunction, aConst, bConst);
		costFunction.put(qFunction, attrCostFunction, prefInfo.getScalingConst(qFunction.getName()));
	}
}
