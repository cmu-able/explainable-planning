package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Location;
import examples.mobilerobot.factors.MoveToAction;
import examples.mobilerobot.factors.RobotBumped;
import examples.mobilerobot.factors.RobotBumpedActionDescription;
import examples.mobilerobot.factors.RobotLocationActionDescription;
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
import preferences.CostFunction;
import preferences.ILinearCostFunction;
import prismconnector.PrismMDPTranslator;

class MobileRobotTest {

	private StateVarDefinition<Location> rLocDef;
	private Location locL1;
	private Location locL2;
	private StateVarDefinition<RobotBumped> rBumpedDef;
	private RobotBumped bumped;
	private RobotBumped notBumped;
	private ActionDefinition<MoveToAction> moveToDef;
	private MoveToAction moveToL1;
	private MoveToAction moveToL2;

	MobileRobotTest() {

	}

	@Test
	void test() {
		fail("Not yet implemented");
	}

	@Test
	public void testMDPConstructor() {
		try {
			XMDP xmdp = createXMDP();
		} catch (AttributeNameNotFoundException | IncompatibleVarException | IncompatibleEffectClassException
				| IncompatibleDiscriminantClassException | IncompatibleActionException e) {
			fail("Exception thrown");
			e.printStackTrace();
		}
	}

	@Test
	public void testPrismMDPTranslator() throws AttributeNameNotFoundException, IncompatibleVarException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException, IncompatibleActionException {
		XMDP xmdp = createXMDP();
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true);
		try {
			String mdpTranslation = mdpTranslator.getMDPTranslation();
		} catch (VarNotFoundException | EffectClassNotFoundException | AttributeNameNotFoundException
				| IncompatibleVarException | DiscriminantNotFoundException e) {
			fail("Exception thrown");
			e.printStackTrace();
		}
	}

	private XMDP createXMDP() throws AttributeNameNotFoundException, IncompatibleVarException,
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
		locL1 = new Location("l1", Area.PUBLIC);
		locL2 = new Location("l2", Area.PUBLIC);
		Set<Location> possibleLocs = new HashSet<>();
		possibleLocs.add(locL1);
		possibleLocs.add(locL2);
		rLocDef = new StateVarDefinition<>("rLoc", possibleLocs);
		StateSpace stateSpace = new StateSpace();
		stateSpace.addStateVarDefinition(rLocDef);
		return stateSpace;
	}

	private ActionSpace createActionSpace() {
		StateVar<Location> rLocL1 = new StateVar<>(rLocDef, locL1);
		StateVar<Location> rLocL2 = new StateVar<>(rLocDef, locL2);
		moveToL1 = new MoveToAction(rLocL1);
		moveToL2 = new MoveToAction(rLocL2);
		Set<MoveToAction> moveToActions = new HashSet<>();
		moveToActions.add(moveToL1);
		moveToActions.add(moveToL2);
		moveToDef = new ActionDefinition<>("moveTo", moveToActions);
		ActionSpace actionSpace = new ActionSpace();
		actionSpace.addActionDefinition(moveToDef);
		return actionSpace;
	}

	private State createInitialState() {
		State initialState = new State();
		StateVar<Location> rLocL1 = new StateVar<>(rLocDef, locL1);
		initialState.addStateVar(rLocL1);
		return initialState;
	}

	private State createGoal() {
		State goal = new State();
		StateVar<Location> rLocL2 = new StateVar<>(rLocDef, locL2);
		goal.addStateVar(rLocL2);
		return goal;
	}

	private TransitionFunction createTransitions() throws AttributeNameNotFoundException, IncompatibleVarException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException, IncompatibleActionException {
		Precondition preMoveToL1 = new Precondition();
		preMoveToL1.add(rLocDef, locL2);
		Precondition preMoveToL2 = new Precondition();
		preMoveToL2.add(rLocDef, locL1);
		RobotLocationActionDescription rLocActionDesc = new RobotLocationActionDescription(moveToDef, rLocDef);
		rLocActionDesc.put(moveToL1, preMoveToL1);
		rLocActionDesc.put(moveToL2, preMoveToL2);
		RobotBumpedActionDescription rBumpedActionDesc = new RobotBumpedActionDescription(moveToDef, rLocDef,
				rBumpedDef);
		rBumpedActionDesc.put(moveToL1, preMoveToL1);
		rBumpedActionDesc.put(moveToL2, preMoveToL2);
		FactoredPSO<MoveToAction> moveToPSO = new FactoredPSO<>(moveToDef);
		moveToPSO.putPrecondition(moveToL1, preMoveToL1);
		moveToPSO.putPrecondition(moveToL2, preMoveToL2);
		moveToPSO.addActionDescription(rLocActionDesc);
		moveToPSO.addActionDescription(rBumpedActionDesc);
		TransitionFunction transFunction = new TransitionFunction();
		transFunction.add(moveToPSO);
		return transFunction;
	}

	private Set<IQFunction> createQFunctions() {
		Set<IQFunction> qFunctions = new HashSet<>();
		// TODO
		return qFunctions;
	}

	private CostFunction createCostFunction() {
		Map<IQFunction, ILinearCostFunction> linearCostFuns = new HashMap<>();
		Map<IQFunction, Double> scalingConsts = new HashMap<>();
		CostFunction costFunction = new CostFunction(linearCostFuns, scalingConsts);
		// TODO
		return costFunction;
	}

}
