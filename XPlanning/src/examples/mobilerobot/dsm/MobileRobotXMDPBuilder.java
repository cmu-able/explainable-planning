package examples.mobilerobot.dsm;

import java.util.HashSet;
import java.util.Set;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotSpeed;
import examples.mobilerobot.factors.SetSpeedAction;
import factors.ActionDefinition;
import factors.StateVarDefinition;
import mdp.ActionSpace;
import mdp.State;
import mdp.StateSpace;
import mdp.TransitionFunction;
import mdp.XMDP;
import metrics.IQFunction;
import objectives.CostFunction;

public class MobileRobotXMDPBuilder {

	// Robot's location state variable
	private StateVarDefinition<Location> rLocDef;

	// Speed settings (known, fixed)
	private RobotSpeed halfSpeed = new RobotSpeed(0.35);
	private RobotSpeed fullSpeed = new RobotSpeed(0.7);

	// Robot's speed state variable
	private StateVarDefinition<RobotSpeed> rSpeedDef;

	// Bump sensor values (known, fixed)
	private RobotBumped bumped = new RobotBumped(true);
	private RobotBumped notBumped = new RobotBumped(false);

	// Robot's bump sensor state variable
	private StateVarDefinition<RobotBumped> rBumpedDef;

	public MobileRobotXMDPBuilder() {

	}

	public XMDP buildXMDP(MapTopology map) {
		constructKnownVariableDefinitions();
		StateSpace stateSpace = buildStateSpace(map);
		ActionSpace actionSpace = buildActionSpace(map);
		State initialState = buildInitialState();
		State goal = buildGoal();
		TransitionFunction transFunction = buildTransitionFunction();
		Set<IQFunction> qFunctions = buildQFunctions();
		CostFunction costFunction = buildCostFunction();
		return new XMDP(stateSpace, actionSpace, initialState, goal, transFunction, qFunctions, costFunction);
	}

	private void constructKnownVariableDefinitions() {
		rSpeedDef = new StateVarDefinition<>("rSpeed", halfSpeed, fullSpeed);
		rBumpedDef = new StateVarDefinition<>("rBumped", bumped, notBumped);
	}

	private StateSpace buildStateSpace(MapTopology map) {
		Set<Location> locs = new HashSet<>();
		for (LocationNode node : map) {
			Location loc = new Location(node.getNodeID(), Area.PUBLIC);
			locs.add(loc);
		}

		rLocDef = new StateVarDefinition<>("rLoc", locs);

		StateSpace stateSpace = new StateSpace();
		stateSpace.addStateVarDefinition(rLocDef);
		stateSpace.addStateVarDefinition(rSpeedDef);
		stateSpace.addStateVarDefinition(rBumpedDef);
		return stateSpace;
	}

	private ActionSpace buildActionSpace(MapTopology map) {
		// MoveTo actions
		Set<MoveToAction> moveTos = new HashSet<>();
		for (Location loc : rLocDef.getPossibleValues()) {
			MoveToAction moveTo = new MoveToAction(rLocDef.getStateVar(loc));
			// TODO: put distance
			moveTos.add(moveTo);
		}

		// MoveTo action definition
		ActionDefinition<MoveToAction> moveToDef = new ActionDefinition<>("moveTo", moveTos);

		// SetSpeed actions
		SetSpeedAction setSpeedHalf = new SetSpeedAction(rSpeedDef.getStateVar(halfSpeed));
		SetSpeedAction setSpeedFull = new SetSpeedAction(rSpeedDef.getStateVar(fullSpeed));

		// SetSpeed action definition
		ActionDefinition<SetSpeedAction> setSpeedDef = new ActionDefinition<>("setSpeed", setSpeedHalf, setSpeedFull);

		ActionSpace actionSpace = new ActionSpace();
		actionSpace.addActionDefinition(moveToDef);
		actionSpace.addActionDefinition(setSpeedDef);
		return actionSpace;
	}

	private State buildInitialState() {
		return null;
	}

	private State buildGoal() {
		return null;
	}

	private TransitionFunction buildTransitionFunction() {
		return null;
	}

	private Set<IQFunction> buildQFunctions() {
		return null;
	}

	private CostFunction buildCostFunction() {
		return null;
	}
}
