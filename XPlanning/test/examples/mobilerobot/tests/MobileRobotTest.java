package examples.mobilerobot.tests;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import examples.mobilerobot.factors.Area;
import examples.mobilerobot.factors.Location;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectClassNotFoundException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.IFactoredPSO;
import mdp.StateSpace;
import mdp.XMDP;
import metrics.IQFunction;
import preferences.CostFunction;
import preferences.ILinearCostFunction;
import prismconnector.PrismMDPTranslator;

class MobileRobotTest {

	@Test
	void test() {
		fail("Not yet implemented");
	}

	@Test
	public void testPrismMDPTranslator() {
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

	private XMDP createXMDP() {
		StateSpace stateSpace = createStateSpace();
		Set<ActionDefinition<IAction>> actionDefs = createActionDefs();
		Set<StateVar<IStateVarValue>> initialState = createInitialState();
		Set<StateVar<IStateVarValue>> goal = createGoal();
		Map<IAction, IFactoredPSO> transitions = createTransitions();
		Set<IQFunction> qFunctions = createQFunctions();
		CostFunction costFunction = createCostFunction();
		XMDP xmdp = new XMDP(stateSpace, actionDefs, initialState, goal, transitions, qFunctions, costFunction);
		return xmdp;
	}

	private StateSpace createStateSpace() {
		Set<Location> possibleLocs = new HashSet<>();
		Location locL1 = new Location("l1", Area.PUBLIC);
		Location locL2 = new Location("l2", Area.PUBLIC);
		possibleLocs.add(locL1);
		possibleLocs.add(locL2);
		StateVarDefinition<Location> rLocDef = new StateVarDefinition<>("rLoc", possibleLocs);
		StateSpace stateSpace = new StateSpace();
		stateSpace.addStateVarDefinition(rLocDef);
		return stateSpace;
	}

	private Set<ActionDefinition<IAction>> createActionDefs() {
		Set<ActionDefinition<IAction>> actionDefs = new HashSet<>();
		// TODO
		return actionDefs;
	}

	private Set<StateVar<IStateVarValue>> createInitialState() {
		Set<StateVar<IStateVarValue>> initialState = new HashSet<>();
		// TODO
		return initialState;
	}

	private Set<StateVar<IStateVarValue>> createGoal() {
		Set<StateVar<IStateVarValue>> goal = new HashSet<>();
		// TODO
		return goal;
	}

	private Map<IAction, IFactoredPSO> createTransitions() {
		Map<IAction, IFactoredPSO> transitions = new HashMap<>();
		// TODO
		return transitions;
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
