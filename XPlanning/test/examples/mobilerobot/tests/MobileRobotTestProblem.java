package examples.mobilerobot.tests;

import java.util.HashSet;
import java.util.Set;

import dtmc.XDTMC;
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
import examples.mobilerobot.metrics.TravelTimeQFunction;
import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.ActionSpace;
import mdp.FactoredPSO;
import mdp.Precondition;
import mdp.State;
import mdp.StateSpace;
import mdp.TransitionFunction;
import mdp.XMDP;
import metrics.IQFunction;
import objectives.AttributeCostFunction;
import objectives.CostFunction;
import policy.Policy;

public class MobileRobotTestProblem {

	private static final double MAX_TOTAL_TIME = 100;

	// Locations
	private Location locL1 = new Location("1", Area.PUBLIC);
	private Location locL2 = new Location("2", Area.PUBLIC);

	// Robot's location state variable
	private StateVarDefinition<Location> rLocDef;

	// Speed settings
	private RobotSpeed halfSpeed = new RobotSpeed(0.35);
	private RobotSpeed fullSpeed = new RobotSpeed(0.7);

	// Robot's speed state variable
	private StateVarDefinition<RobotSpeed> rSpeedDef = new StateVarDefinition<>("rSpeed", halfSpeed, fullSpeed);

	// Bump sensor values
	private RobotBumped bumped = new RobotBumped(true);
	private RobotBumped notBumped = new RobotBumped(false);

	// Robot's bump sensor state variable
	private StateVarDefinition<RobotBumped> rBumpedDef = new StateVarDefinition<>("rBumped", bumped, notBumped);

	// MoveTo actions
	private MoveToAction moveToL1 = new MoveToAction(rLocDef.getStateVar(locL1));
	private MoveToAction moveToL2 = new MoveToAction(rLocDef.getStateVar(locL2));

	// MoveTo action definition
	private ActionDefinition<MoveToAction> moveToDef;

	// SetSpeed actions
	private SetSpeedAction setSpeedHalf = new SetSpeedAction(rSpeedDef.getStateVar(halfSpeed));
	private SetSpeedAction setSpeedFull = new SetSpeedAction(rSpeedDef.getStateVar(fullSpeed));

	// SetSpeed action definition
	private ActionDefinition<SetSpeedAction> setSpeedDef = new ActionDefinition<>("setSpeed", setSpeedHalf,
			setSpeedFull);

	// QA functions
	TravelTimeQFunction timeQFunction = new TravelTimeQFunction(rLocDef, rSpeedDef, moveToDef, rLocDef);

	// Single-attribute cost functions
	private AttributeCostFunction<TravelTimeQFunction> timeCostFunction = new AttributeCostFunction<TravelTimeQFunction>(
			timeQFunction, 0, 1 / MAX_TOTAL_TIME);

	public MobileRobotTestProblem() {

	}

	public XMDP createXMDP() throws AttributeNameNotFoundException, IncompatibleVarException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException, IncompatibleActionException {
		StateSpace stateSpace = createStateSpace();
		ActionSpace actionSpace = createActionSpace();
		State initialState = createInitialState();
		State goal = createGoal();
		TransitionFunction transFunction = createTransitions();
		Set<IQFunction> qFunctions = createQFunctions();
		CostFunction costFunction = createCostFunction();
		XMDP xmdp = new XMDP(stateSpace, actionSpace, initialState, goal, transFunction, qFunctions, costFunction);
		return xmdp;
	}

	private StateSpace createStateSpace() {
		rLocDef = new StateVarDefinition<>("rLoc", locL1, locL2);

		StateSpace stateSpace = new StateSpace();
		stateSpace.addStateVarDefinition(rLocDef);
		stateSpace.addStateVarDefinition(rSpeedDef);
		return stateSpace;
	}

	private ActionSpace createActionSpace() {
		Distance distanceL1L2 = new Distance(10);
		Distance distanceL2L1 = new Distance(10);
		Occlusion occlusionL1L2 = Occlusion.CLEAR;
		Occlusion occlusionL2L1 = Occlusion.CLEAR;

		// MoveTo actions
		moveToL1 = new MoveToAction(rLocDef.getStateVar(locL1));
		moveToL2 = new MoveToAction(rLocDef.getStateVar(locL2));

		moveToL1.putDistanceValue(distanceL2L1, rLocDef.getStateVar(locL2));
		moveToL2.putDistanceValue(distanceL1L2, rLocDef.getStateVar(locL1));

		moveToL1.putOcclusionValue(occlusionL2L1, rLocDef.getStateVar(locL2));
		moveToL2.putOcclusionValue(occlusionL1L2, rLocDef.getStateVar(locL1));

		// MoveTo action definition
		moveToDef = new ActionDefinition<>("moveTo", moveToL1, moveToL2);

		ActionSpace actionSpace = new ActionSpace();
		actionSpace.addActionDefinition(moveToDef);
		actionSpace.addActionDefinition(setSpeedDef);
		return actionSpace;
	}

	private State createInitialState() {
		State initialState = new State();
		initialState.addStateVar(rLocDef.getStateVar(locL1));
		initialState.addStateVar(rSpeedDef.getStateVar(halfSpeed));
		return initialState;
	}

	private State createGoal() {
		State goal = new State();
		goal.addStateVar(rLocDef.getStateVar(locL2));
		return goal;
	}

	private TransitionFunction createTransitions() throws AttributeNameNotFoundException, IncompatibleVarException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException, IncompatibleActionException {
		// MoveTo:
		// Precondition
		Precondition preMoveToL1 = new Precondition();
		preMoveToL1.add(rLocDef, locL2);
		Precondition preMoveToL2 = new Precondition();
		preMoveToL2.add(rLocDef, locL1);

		// Action description
		RobotLocationActionDescription rLocActionDesc = new RobotLocationActionDescription(moveToDef, rLocDef);
		rLocActionDesc.put(moveToL1, preMoveToL1);
		rLocActionDesc.put(moveToL2, preMoveToL2);

		// PSO
		FactoredPSO<MoveToAction> moveToPSO = new FactoredPSO<>(moveToDef);
		moveToPSO.putPrecondition(moveToL1, preMoveToL1);
		moveToPSO.putPrecondition(moveToL2, preMoveToL2);
		moveToPSO.addActionDescription(rLocActionDesc);

		// SetSpeed:
		// Precondition
		Precondition preSetSpeedHalf = new Precondition();
		preSetSpeedHalf.add(rSpeedDef, fullSpeed);
		Precondition preSetSpeedFull = new Precondition();
		preSetSpeedFull.add(rSpeedDef, halfSpeed);

		// Action description
		RobotSpeedActionDescription rSpeedActionDesc = new RobotSpeedActionDescription(setSpeedDef, rSpeedDef);
		rSpeedActionDesc.put(setSpeedHalf, preSetSpeedHalf);
		rSpeedActionDesc.put(setSpeedFull, preSetSpeedFull);

		// PSO
		FactoredPSO<SetSpeedAction> setSpeedPSO = new FactoredPSO<>(setSpeedDef);
		setSpeedPSO.putPrecondition(setSpeedHalf, preSetSpeedHalf);
		setSpeedPSO.putPrecondition(setSpeedFull, preSetSpeedFull);
		setSpeedPSO.addActionDescription(rSpeedActionDesc);

		TransitionFunction transFunction = new TransitionFunction();
		transFunction.add(moveToPSO);
		transFunction.add(setSpeedPSO);
		return transFunction;
	}

	private Set<IQFunction> createQFunctions() {
		Set<IQFunction> qFunctions = new HashSet<>();
		qFunctions.add(timeQFunction);
		return qFunctions;
	}

	private CostFunction createCostFunction() {
		CostFunction costFunction = new CostFunction();
		costFunction.put(timeQFunction, timeCostFunction, 1.0);
		return costFunction;
	}

	public XDTMC createXDTMC()
			throws AttributeNameNotFoundException, IncompatibleVarException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, IncompatibleActionException, ActionDefinitionNotFoundException,
			EffectClassNotFoundException, VarNotFoundException, ActionNotFoundException, DiscriminantNotFoundException {
		XMDP xmdp = createXMDP();
		Policy policy = createPolicy();
		XDTMC xdtmc = new XDTMC(xmdp, policy);
		return xdtmc;
	}

	private Policy createPolicy() {
		Policy policy = new Policy();
		State iniState = new State();
		State finalState = new State();
		StateVar<Location> rLocL1 = rLocDef.getStateVar(locL1);
		StateVar<Location> rLocL2 = rLocDef.getStateVar(locL2);
		StateVar<RobotSpeed> rSpeedHalf = rSpeedDef.getStateVar(halfSpeed);
		iniState.addStateVar(rLocL1);
		iniState.addStateVar(rSpeedHalf);
		finalState.addStateVar(rLocL2);
		policy.put(iniState, moveToL2);
		policy.put(finalState, moveToL1);
		return policy;
	}
}
