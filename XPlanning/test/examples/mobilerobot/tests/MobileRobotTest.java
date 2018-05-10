package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
import exceptions.ResultParsingException;
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
import policy.Policy;
import preferences.AttributeCostFunction;
import preferences.CostFunction;
import prism.PrismException;
import prismconnector.PrismConnector;
import prismconnector.PrismDTMCTranslator;
import prismconnector.PrismExplicitModelPointer;
import prismconnector.PrismMDPTranslator;

class MobileRobotTest {

	private StateVarDefinition<Location> rLocDef;
	private Location locL1;
	private Location locL2;
	private StateVarDefinition<RobotSpeed> rSpeedDef;
	private RobotSpeed halfSpeed;
	private RobotSpeed fullSpeed;
	private StateVarDefinition<RobotBumped> rBumpedDef;
	private RobotBumped bumped;
	private RobotBumped notBumped;
	private ActionDefinition<MoveToAction> moveToDef;
	private MoveToAction moveToL1;
	private MoveToAction moveToL2;
	private ActionDefinition<SetSpeedAction> setSpeedDef;
	private SetSpeedAction setSpeedHalf;
	private SetSpeedAction setSpeedFull;
	private TravelTimeQFunction timeQFunction;
	private AttributeCostFunction<TravelTimeQFunction> timeCostFunction;

	MobileRobotTest() {

	}

	@Test
	public void testMDPConstructor() {
		try {
			XMDP xmdp = createXMDP();
		} catch (AttributeNameNotFoundException | IncompatibleVarException | IncompatibleEffectClassException
				| IncompatibleDiscriminantClassException | IncompatibleActionException e) {
			e.printStackTrace();
			fail("Exception thrown while creating XMDP");
		}
	}

	@Test
	public void testPrismMDPTranslator() throws AttributeNameNotFoundException, IncompatibleVarException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException, IncompatibleActionException {
		XMDP xmdp = createXMDP();
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true);

		try {
			String mdpWithQAs = mdpTranslator.getMDPTranslationWithQAs();
			String goalProperty = mdpTranslator.getGoalPropertyTranslation();
			System.out.println("MDP Translation (with QAs):");
			System.out.println(mdpWithQAs);
			System.out.println();
			System.out.println("Goal Property Translation:");
			System.out.println(goalProperty);
			System.out.println();
		} catch (VarNotFoundException | EffectClassNotFoundException | AttributeNameNotFoundException
				| IncompatibleVarException | DiscriminantNotFoundException | ActionNotFoundException
				| ActionDefinitionNotFoundException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XMDP to PRISM MDP");
		}
	}

	@Test
	public void testDTMCConstructor() {
		try {
			XDTMC xdtmc = createXDTMC();
		} catch (AttributeNameNotFoundException | IncompatibleVarException | IncompatibleEffectClassException
				| IncompatibleDiscriminantClassException | IncompatibleActionException
				| ActionDefinitionNotFoundException | EffectClassNotFoundException | VarNotFoundException
				| ActionNotFoundException | DiscriminantNotFoundException e) {
			e.printStackTrace();
			fail("Exception thrown while creating XDTMC");
		}
	}

	@Test
	public void testPrismDTMCTranslator()
			throws AttributeNameNotFoundException, IncompatibleVarException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, IncompatibleActionException, ActionDefinitionNotFoundException,
			EffectClassNotFoundException, VarNotFoundException, ActionNotFoundException, DiscriminantNotFoundException {
		XDTMC xdtmc = createXDTMC();

		try {
			PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true);
			String dtmcWithQAs = dtmcTranslator.getDTMCTranslationWithQAs();
			String timeQueryTranslation = dtmcTranslator.getNumQueryPropertyTranslation(timeQFunction);
			System.out.println("DTMC Translation (with QAs):");
			System.out.println(dtmcWithQAs);
			System.out.println();
			System.out.println("Time Query Property Translation:");
			System.out.println(timeQueryTranslation);
			System.out.println();
		} catch (ActionDefinitionNotFoundException | EffectClassNotFoundException | VarNotFoundException
				| ActionNotFoundException | DiscriminantNotFoundException e) {
			e.printStackTrace();
			fail("Exception thrown while translating XDTMC to PRISM DTMC");
		}
	}

	@Test
	public void testPrismMDPAdversaryGeneration() throws AttributeNameNotFoundException, IncompatibleVarException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException, IncompatibleActionException,
			VarNotFoundException, EffectClassNotFoundException, DiscriminantNotFoundException, ActionNotFoundException,
			ActionDefinitionNotFoundException {
		String outputPath = "/Users/rsukkerd/Projects/explainable-planning/models/test0/output";
		String staOutputFilename = "adv.sta";
		String traOutputFilename = "adv.tra";
		String labOutputFilename = "adv.lab";
		String srewOutputFilename = "adv.srew";

		XMDP xmdp = createXMDP();
		PrismMDPTranslator mdpTranslator = new PrismMDPTranslator(xmdp, true);
		String mdpWithQAs = mdpTranslator.getMDPTranslationWithQAs();
		String goalProperty = mdpTranslator.getGoalPropertyTranslation();

		try {
			PrismConnector connector = new PrismConnector();
			double totalCost = connector.generateMDPAdversary(mdpWithQAs, goalProperty, outputPath, staOutputFilename,
					traOutputFilename, labOutputFilename, srewOutputFilename);
			System.out.print("Expected total cost of adversary: ");
			System.out.println(totalCost);
			System.out.println();
		} catch (FileNotFoundException | PrismException | ResultParsingException e) {
			e.printStackTrace();
			fail("Exception thrown while PRISM generating MDP adversary");
		}
	}

	@Test
	public void testPrismExplicitDTMCPropertyQuery() {
		String modelPath = "/Users/rsukkerd/Projects/explainable-planning/models/test0/test_output";
		String staFilename = "adv.sta";
		String traFilename = "adv.tra";
		String labFilename = "adv.lab";
		String srewFilename = "adv.srew";
		PrismExplicitModelPointer explicitModelPointer = new PrismExplicitModelPointer(modelPath, staFilename,
				traFilename, labFilename, srewFilename, 2);
		String propertyStr = "R=? [ F rLoc=0 & readyToCopy ]";

		try {
			PrismConnector connector = new PrismConnector();
			double totalCost = connector.queryPropertyFromExplicitDTMC(propertyStr, explicitModelPointer, 1);
			double totalTime = connector.queryPropertyFromExplicitDTMC(propertyStr, explicitModelPointer, 2);
			System.out.print("Query property: ");
			System.out.println(propertyStr);
			System.out.print("Expected total cost of adversary: ");
			System.out.println(totalCost);
			System.out.print("Expected total time of adversary: ");
			System.out.println(totalTime);
			System.out.println();
		} catch (PrismException | ResultParsingException e) {
			e.printStackTrace();
			fail("Exception thrown while PRISM model checking DTCM property");
		}
	}

	@Test
	public void testPrismDTMCPropertyQuery()
			throws AttributeNameNotFoundException, IncompatibleVarException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, IncompatibleActionException, ActionDefinitionNotFoundException,
			EffectClassNotFoundException, VarNotFoundException, ActionNotFoundException, DiscriminantNotFoundException {
		XDTMC xdtmc = createXDTMC();
		PrismDTMCTranslator dtmcTranslator = new PrismDTMCTranslator(xdtmc, true);
		String dtmcWithQAs = dtmcTranslator.getDTMCTranslationWithQAs();
		String timeQuery = dtmcTranslator.getNumQueryPropertyTranslation(timeQFunction);

		try {
			PrismConnector connector = new PrismConnector();
			double result = connector.queryPropertyFromDTMC(dtmcWithQAs, timeQuery);
			System.out.print("Query property: ");
			System.out.println(timeQuery);
			System.out.print("Expected total time: ");
			System.out.println(result);
			System.out.println();
		} catch (PrismException | ResultParsingException e) {
			e.printStackTrace();
			fail("Exception thrown while PRISM model checking DTCM property");
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
		locL1 = new Location("1", Area.PUBLIC);
		locL2 = new Location("2", Area.PUBLIC);
		Set<Location> possibleLocs = new HashSet<>();
		possibleLocs.add(locL1);
		possibleLocs.add(locL2);
		rLocDef = new StateVarDefinition<>("rLoc", possibleLocs);

		halfSpeed = new RobotSpeed(0.35);
		fullSpeed = new RobotSpeed(0.7);
		Set<RobotSpeed> possibleSpeedSettings = new HashSet<>();
		possibleSpeedSettings.add(halfSpeed);
		possibleSpeedSettings.add(fullSpeed);
		rSpeedDef = new StateVarDefinition<>("rSpeed", possibleSpeedSettings);

		StateSpace stateSpace = new StateSpace();
		stateSpace.addStateVarDefinition(rLocDef);
		stateSpace.addStateVarDefinition(rSpeedDef);
		return stateSpace;
	}

	private ActionSpace createActionSpace() {
		StateVar<Location> rLocL1 = new StateVar<>(rLocDef, locL1);
		StateVar<Location> rLocL2 = new StateVar<>(rLocDef, locL2);
		Distance distanceL1L2 = new Distance(10);
		Distance distanceL2L1 = new Distance(10);
		Occlusion occlusionL1L2 = Occlusion.CLEAR;
		Occlusion occlusionL2L1 = Occlusion.CLEAR;
		moveToL1 = new MoveToAction(rLocL1);
		moveToL2 = new MoveToAction(rLocL2);

		moveToL1.putDistanceValue(distanceL2L1, rLocL2);
		moveToL2.putDistanceValue(distanceL1L2, rLocL1);

		moveToL1.putOcclusionValue(occlusionL2L1, rLocL2);
		moveToL2.putOcclusionValue(occlusionL1L2, rLocL2);

		Set<MoveToAction> moveToActions = new HashSet<>();
		moveToActions.add(moveToL1);
		moveToActions.add(moveToL2);
		moveToDef = new ActionDefinition<>("moveTo", moveToActions);

		StateVar<RobotSpeed> rSpeedHalf = new StateVar<>(rSpeedDef, halfSpeed);
		StateVar<RobotSpeed> rSpeedFull = new StateVar<>(rSpeedDef, fullSpeed);
		setSpeedHalf = new SetSpeedAction(rSpeedHalf);
		setSpeedFull = new SetSpeedAction(rSpeedFull);
		Set<SetSpeedAction> setSpeedActions = new HashSet<>();
		setSpeedActions.add(setSpeedHalf);
		setSpeedActions.add(setSpeedFull);
		setSpeedDef = new ActionDefinition<>("setSpeed", setSpeedActions);

		ActionSpace actionSpace = new ActionSpace();
		actionSpace.addActionDefinition(moveToDef);
		actionSpace.addActionDefinition(setSpeedDef);
		return actionSpace;
	}

	private State createInitialState() {
		State initialState = new State();
		StateVar<Location> rLocL1 = new StateVar<>(rLocDef, locL1);
		StateVar<RobotSpeed> rSpeedHalf = new StateVar<>(rSpeedDef, halfSpeed);

		initialState.addStateVar(rLocL1);
		initialState.addStateVar(rSpeedHalf);
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
		timeQFunction = new TravelTimeQFunction(rLocDef, rSpeedDef, moveToDef, rLocDef);
		qFunctions.add(timeQFunction);
		return qFunctions;
	}

	private CostFunction createCostFunction() {
		timeCostFunction = new AttributeCostFunction<>(timeQFunction, 1, 0);
		CostFunction costFunction = new CostFunction();
		costFunction.put(timeQFunction, timeCostFunction, 1.0);
		return costFunction;
	}

	private Policy createPolicy() {
		Policy policy = new Policy();
		State iniState = new State();
		State finalState = new State();
		StateVar<Location> rLocL1 = new StateVar<>(rLocDef, locL1);
		StateVar<Location> rLocL2 = new StateVar<>(rLocDef, locL2);
		StateVar<RobotSpeed> rSpeedHalf = new StateVar<>(rSpeedDef, halfSpeed);
		iniState.addStateVar(rLocL1);
		iniState.addStateVar(rSpeedHalf);
		finalState.addStateVar(rLocL2);
		policy.put(iniState, moveToL2);
		policy.put(finalState, moveToL1);
		return policy;
	}

	private XDTMC createXDTMC()
			throws AttributeNameNotFoundException, IncompatibleVarException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, IncompatibleActionException, ActionDefinitionNotFoundException,
			EffectClassNotFoundException, VarNotFoundException, ActionNotFoundException, DiscriminantNotFoundException {
		XMDP xmdp = createXMDP();
		Policy policy = createPolicy();
		XDTMC xdtmc = new XDTMC(xmdp, policy);
		return xdtmc;
	}

}
