package examples.mobilerobot.tests;

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
import examples.mobilerobot.qfactors.RobotLocationActionDescription;
import examples.mobilerobot.qfactors.RobotSpeed;
import examples.mobilerobot.qfactors.RobotSpeedActionDescription;
import examples.mobilerobot.qfactors.SetSpeedAction;
import language.dtmc.XDTMC;
import language.exceptions.XMDPException;
import language.mdp.ActionSpace;
import language.mdp.FactoredPSO;
import language.mdp.Precondition;
import language.mdp.QSpace;
import language.mdp.StateSpace;
import language.mdp.StateVarTuple;
import language.mdp.TransitionFunction;
import language.mdp.XMDP;
import language.metrics.EventBasedMetric;
import language.metrics.NonStandardMetricQFunction;
import language.objectives.AttributeCostFunction;
import language.objectives.CostFunction;
import language.policy.Policy;
import language.qfactors.ActionDefinition;
import language.qfactors.StateVar;
import language.qfactors.StateVarDefinition;

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
	private MoveToAction moveToL1;
	private MoveToAction moveToL2;

	// MoveTo action definition
	private ActionDefinition<MoveToAction> moveToDef;

	// SetSpeed actions
	private SetSpeedAction setSpeedHalf = new SetSpeedAction(rSpeedDef.getStateVar(halfSpeed));
	private SetSpeedAction setSpeedFull = new SetSpeedAction(rSpeedDef.getStateVar(fullSpeed));

	// SetSpeed action definition
	private ActionDefinition<SetSpeedAction> setSpeedDef = new ActionDefinition<>("setSpeed", setSpeedHalf,
			setSpeedFull);

	// QA functions
	TravelTimeQFunction timeQFunction;
	NonStandardMetricQFunction<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> intrusiveQFunction;

	// Single-attribute cost functions
	private AttributeCostFunction<TravelTimeQFunction> timeCostFunction;

	private static final double NON_INTRUSIVE_PENALTY = 0;
	private static final double SEMI_INTRUSIVE_PEANLTY = 1;
	private static final double VERY_INTRUSIVE_PENALTY = 3;

	public MobileRobotTestProblem() {

	}

	public XMDP createXMDP() throws XMDPException {
		StateSpace stateSpace = createStateSpace();
		ActionSpace actionSpace = createActionSpace();
		StateVarTuple initialState = createInitialState();
		StateVarTuple goal = createGoal();
		TransitionFunction transFunction = createTransitions();
		QSpace qSpace = createQFunctions();
		CostFunction costFunction = createCostFunction();
		XMDP xmdp = new XMDP(stateSpace, actionSpace, initialState, goal, transFunction, qSpace, costFunction);
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

	private StateVarTuple createInitialState() {
		StateVarTuple initialState = new StateVarTuple();
		initialState.addStateVar(rLocDef.getStateVar(locL1));
		initialState.addStateVar(rSpeedDef.getStateVar(halfSpeed));
		return initialState;
	}

	private StateVarTuple createGoal() {
		StateVarTuple goal = new StateVarTuple();
		goal.addStateVar(rLocDef.getStateVar(locL2));
		return goal;
	}

	private TransitionFunction createTransitions() throws XMDPException {
		// MoveTo:
		// Precondition
		Precondition<MoveToAction> preMoveTo = new Precondition<>(moveToDef);
		preMoveTo.add(moveToL1, rLocDef, locL2);
		preMoveTo.add(moveToL2, rLocDef, locL1);

		// Action description
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

		// Action description
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

	private QSpace createQFunctions() {
		// Travel time
		TravelTimeDomain timeDomain = new TravelTimeDomain(rLocDef, rSpeedDef, moveToDef, rLocDef);
		timeQFunction = new TravelTimeQFunction(timeDomain);

		// Intrusiveness
		IntrusivenessDomain intrusiveDomain = new IntrusivenessDomain(moveToDef, rLocDef);
		IntrusiveMoveEvent nonIntrusive = new IntrusiveMoveEvent("non-intrusive", intrusiveDomain, Area.PUBLIC);
		IntrusiveMoveEvent somewhatIntrusive = new IntrusiveMoveEvent("somewhat-intrusive", intrusiveDomain,
				Area.SEMI_PRIVATE);
		IntrusiveMoveEvent veryIntrusive = new IntrusiveMoveEvent("very-intrusive", intrusiveDomain, Area.PRIVATE);
		EventBasedMetric<MoveToAction, IntrusivenessDomain, IntrusiveMoveEvent> metric = new EventBasedMetric<>(
				"intrusiveness", intrusiveDomain);
		metric.put(nonIntrusive, NON_INTRUSIVE_PENALTY);
		metric.put(somewhatIntrusive, SEMI_INTRUSIVE_PEANLTY);
		metric.put(veryIntrusive, VERY_INTRUSIVE_PENALTY);
		intrusiveQFunction = new NonStandardMetricQFunction<>(metric);

		QSpace qSpace = new QSpace();
		qSpace.addQFunction(timeQFunction);
		return qSpace;
	}

	private CostFunction createCostFunction() {
		timeCostFunction = new AttributeCostFunction<>(timeQFunction, 0, 1 / MAX_TOTAL_TIME);
		CostFunction costFunction = new CostFunction();
		costFunction.put(timeQFunction, timeCostFunction, 1.0);
		return costFunction;
	}

	public XDTMC createXDTMC() throws XMDPException {
		XMDP xmdp = createXMDP();
		Policy policy = createPolicy();
		XDTMC xdtmc = new XDTMC(xmdp, policy);
		return xdtmc;
	}

	private Policy createPolicy() {
		Policy policy = new Policy();
		StateVarTuple iniState = new StateVarTuple();
		StateVarTuple finalState = new StateVarTuple();
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
