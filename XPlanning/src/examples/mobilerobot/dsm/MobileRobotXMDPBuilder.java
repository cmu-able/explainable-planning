package examples.mobilerobot.dsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import examples.mobilerobot.dsm.exceptions.MapTopologyException;
import examples.mobilerobot.dsm.exceptions.NodeAttributeNotFoundException;
import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Distance;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.Occlusion;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotLocationActionDescription;
import examples.mobilerobot.factors.RobotSpeed;
import examples.mobilerobot.factors.RobotSpeedActionDescription;
import examples.mobilerobot.factors.SetSpeedAction;
import examples.mobilerobot.metrics.TravelTimeDomain;
import examples.mobilerobot.metrics.TravelTimeQFunction;
import factors.ActionDefinition;
import factors.StateVarDefinition;
import language.exceptions.IncompatibleActionException;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import mdp.ActionSpace;
import mdp.FactoredPSO;
import mdp.Precondition;
import mdp.QSpace;
import mdp.StateSpace;
import mdp.StateVarTuple;
import mdp.TransitionFunction;
import mdp.XMDP;

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
	// Speed settings (known, fixed)
	private RobotSpeed halfSpeed = new RobotSpeed(0.35);
	private RobotSpeed fullSpeed = new RobotSpeed(0.7);

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

	// --- Travel time --- //
	private TravelTimeQFunction timeQFunction;
	// ------ //

	// Map location nodes to the corresponding location values
	// To be used when adding derived attribute values to move-actions
	private Map<LocationNode, Location> mLocMap = new HashMap<>();

	public MobileRobotXMDPBuilder() {
		// Constructor may take as input other DSMs
	}

	public XMDP buildXMDP(MapTopology map, LocationNode startNode, LocationNode goalNode, double maxTravelTime)
			throws IncompatibleActionException, MapTopologyException {
		StateSpace stateSpace = buildStateSpace(map);
		ActionSpace actionSpace = buildActionSpace(map);
		StateVarTuple initialState = buildInitialState(startNode);
		StateVarTuple goal = buildGoal(goalNode);
		TransitionFunction transFunction = buildTransitionFunction(map);
		QSpace qSpace = buildQFunctions();
		CostFunction costFunction = buildCostFunction(maxTravelTime);
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
		return initialState;
	}

	private StateVarTuple buildGoal(LocationNode goalNode) {
		Location loc = mLocMap.get(goalNode);
		StateVarTuple goal = new StateVarTuple();
		goal.addStateVar(rLocDef.getStateVar(loc));
		return goal;
	}

	private TransitionFunction buildTransitionFunction(MapTopology map)
			throws IncompatibleActionException, MapTopologyException {
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
		timeQFunction = new TravelTimeQFunction(timeDomain);

		QSpace qSpace = new QSpace();
		qSpace.addQFunction(timeQFunction);
		return qSpace;
	}

	private CostFunction buildCostFunction(double maxTravelTime) {
		AttributeCostFunction<TravelTimeQFunction> timeCostFunction = new AttributeCostFunction<>(timeQFunction, 0,
				1 / maxTravelTime);
		CostFunction costFunction = new CostFunction();
		costFunction.put(timeQFunction, timeCostFunction, 1.0);
		return costFunction;
	}
}
