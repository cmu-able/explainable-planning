package prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import factors.IAction;
import factors.IStateVarBoolean;
import factors.IStateVarDouble;
import factors.IStateVarInt;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.ActionDescription;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.EffectClass;
import mdp.FactoredPSO;
import mdp.IActionDescription;
import mdp.ProbabilisticEffect;
import mdp.ProbabilisticTransition;
import mdp.State;
import mdp.StateSpace;
import mdp.TransitionFunction;
import mdp.XMDP;

public class PrismMDPTranslator {

	private static final String INDENT = "  ";
	private static final String SRC_SUFFIX = "Src";

	private XMDP mXMDP;
	private boolean mThreeParamRewards;
	private PrismTranslatorUtilities mUtilities;

	private String currModuleName;

	public PrismMDPTranslator(XMDP xmdp, boolean threeParamRewards) {
		mXMDP = xmdp;
		mThreeParamRewards = threeParamRewards;
		ValueEncodingScheme encodings;
		if (threeParamRewards) {
			encodings = new ValueEncodingScheme(xmdp.getStateSpace(), xmdp.getActionSpace());
		} else {
			encodings = new ValueEncodingScheme(xmdp.getStateSpace());
		}
		mUtilities = new PrismTranslatorUtilities(encodings, threeParamRewards);
	}

	public String getMDPTranslation() throws VarNotFoundException, EffectClassNotFoundException,
			AttributeNameNotFoundException, IncompatibleVarException, DiscriminantNotFoundException,
			ActionNotFoundException, IncompatibleActionException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException {
		String constsDecl = buildConstsDecl(mXMDP.getStateSpace());
		String modules = buildModules();
		String rewards = mUtilities.buildRewards(mXMDP.getTransitionFunction(), mXMDP.getQFunctions(),
				mXMDP.getCostFunction());
		StringBuilder builder = new StringBuilder();
		builder.append("mdp");
		builder.append("\n\n");
		builder.append(constsDecl);
		builder.append("\n");
		builder.append(modules);
		builder.append("\n");
		builder.append(rewards);
		return builder.toString();
	}

	/**
	 * 
	 * @return P>=1 [ F "{varName}={encoded int value} & ..." ]
	 * @throws VarNotFoundException
	 */
	public String getGoalPropertyTranslation() throws VarNotFoundException {
		State goal = mXMDP.getGoal();
		StringBuilder builder = new StringBuilder();
		builder.append("P>=1 [ F \"");
		boolean firstVar = true;
		for (StateVar<IStateVarValue> goalVar : goal) {
			Integer encodedValue = mUtilities.getValueEncodingScheme().getEncodedIntValue(goalVar.getDefinition(),
					goalVar.getValue());
			if (!firstVar) {
				builder.append(" & ");
			} else {
				firstVar = false;
			}
			builder.append(goalVar.getName());
			builder.append("=");
			builder.append(encodedValue);
		}
		builder.append("\" ]");
		return builder.toString();
	}

	/**
	 * 
	 * @param stateSpace
	 *            Definitions of all state variables
	 * @return const int {varName}_{value} = {encoded int value}; ...
	 * @throws VarNotFoundException
	 */
	private String buildConstsDecl(StateSpace stateSpace) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		for (StateVarDefinition<IStateVarValue> stateVarDef : stateSpace) {
			String varName = stateVarDef.getName();
			builder.append("// Possible values of ");
			builder.append(varName);
			builder.append("\n");
			for (IStateVarValue value : stateVarDef.getPossibleValues()) {
				if (value instanceof IStateVarBoolean || value instanceof IStateVarInt) {
					continue;
				}
				Integer encodedValue = mUtilities.getValueEncodingScheme().getEncodedIntValue(stateVarDef, value);
				builder.append("const int ");
				builder.append(varName);
				builder.append("_");
				String valueString = String.valueOf(value);
				builder.append(sanitizeNameString(valueString));
				builder.append(" = ");
				builder.append(encodedValue);
				builder.append(";");
				builder.append("\n");
			}
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param stateVarDef
	 * @return formula {varName}Double = ({varName}={encoded int}) ? {double value} : ... : -1;
	 * @throws VarNotFoundException
	 */
	private String buildVarDoubleFormula(StateVarDefinition<IStateVarDouble> stateVarDef) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		String formulaName = stateVarDef.getName() + "Double";
		builder.append("formula ");
		builder.append(formulaName);
		builder.append(" = ");
		for (IStateVarDouble value : stateVarDef.getPossibleValues()) {
			Integer encodedInt = mUtilities.getValueEncodingScheme().getEncodedIntValue(stateVarDef, value);
			builder.append("(");
			builder.append(stateVarDef.getName());
			builder.append("=");
			builder.append(encodedInt);
			builder.append(") ? ");
			builder.append(value.getValue());
			builder.append(" : ");
		}
		builder.append("-1;");
		return builder.toString();
	}

	/**
	 * 
	 * @return module {name} {vars decl} {commands} endmodule ...
	 * @throws VarNotFoundException
	 * @throws EffectClassNotFoundException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleVarException
	 * @throws IncompatibleActionException
	 * @throws ActionNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 */
	private String buildModules() throws VarNotFoundException, EffectClassNotFoundException, ActionNotFoundException,
			IncompatibleActionException, IncompatibleVarException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, ActionDefinitionNotFoundException {
		Set<Map<EffectClass, FactoredPSO<IAction>>> chainsOfEffectClasses = getChainsOfEffectClassesHelper();
		StringBuilder builder = new StringBuilder();
		int moduleCount = 0;

		for (Map<EffectClass, FactoredPSO<IAction>> chain : chainsOfEffectClasses) {
			moduleCount++;
			StateSpace moduleVarSpace = new StateSpace();
			Map<FactoredPSO<IAction>, Set<EffectClass>> actionPSOs = new HashMap<>();

			for (Entry<EffectClass, FactoredPSO<IAction>> entry : chain.entrySet()) {
				EffectClass effectClass = entry.getKey();
				FactoredPSO<IAction> actionPSO = entry.getValue();

				moduleVarSpace.addStateVarDefinitions(effectClass);

				if (!actionPSOs.containsKey(actionPSO)) {
					actionPSOs.put(actionPSO, new HashSet<>());
				}
				actionPSOs.get(actionPSO).add(effectClass);
			}

			String module = buildModule("module_" + moduleCount, moduleVarSpace, actionPSOs);
			builder.append(module);
			builder.append("\n\n");
		}

		if (mThreeParamRewards) {
			String helperModule = mUtilities.buildHelperModule(mXMDP.getStateSpace(), mXMDP.getInitialState(),
					mXMDP.getActionSpace(), mXMDP.getTransitionFunction());
			builder.append(helperModule);
			builder.append("\n\n");
		}

		return builder.toString();
	}

	private Set<Map<EffectClass, FactoredPSO<IAction>>> getChainsOfEffectClassesHelper() {
		Set<Map<EffectClass, FactoredPSO<IAction>>> currChains = new HashSet<>();
		TransitionFunction transFunction = mXMDP.getTransitionFunction();
		boolean firstPSO = true;
		for (FactoredPSO<IAction> actionPSO : transFunction) {
			Set<EffectClass> actionEffectClasses = actionPSO.getIndependentEffectClasses();
			if (firstPSO) {
				for (EffectClass effectClass : actionEffectClasses) {
					Map<EffectClass, FactoredPSO<IAction>> iniChain = new HashMap<>();
					iniChain.put(effectClass, actionPSO);
					currChains.add(iniChain);
				}
				firstPSO = false;
				continue;
			}
			for (EffectClass effectClass : actionEffectClasses) {
				currChains = getChainsOfEffectClasses(currChains, effectClass, actionPSO);
			}
		}
		return currChains;
	}

	private Set<Map<EffectClass, FactoredPSO<IAction>>> getChainsOfEffectClasses(
			Set<Map<EffectClass, FactoredPSO<IAction>>> currChains, EffectClass effectClass,
			FactoredPSO<IAction> actionPSO) {
		Set<Map<EffectClass, FactoredPSO<IAction>>> res = new HashSet<>();
		Map<EffectClass, FactoredPSO<IAction>> newChain = new HashMap<>();
		for (Map<EffectClass, FactoredPSO<IAction>> chain : currChains) {
			boolean overlapped = false;
			for (EffectClass currEffectClass : chain.keySet()) {
				if (effectClass.overlaps(currEffectClass)) {
					newChain.putAll(chain);
					overlapped = true;
					break;
				}
			}
			if (!overlapped) {
				res.add(chain);
			}
		}
		newChain.put(effectClass, actionPSO);
		res.add(newChain);
		return res;
	}

	/**
	 * 
	 * @param moduleName
	 *            A unique name of the module
	 * @param moduleVarSpace
	 *            Variables of the module
	 * @param actionPSOs
	 *            A mapping from each action PSO to (a subset of) its effect classes that are "chained" by other effect
	 *            classes of other action types
	 * @return module {name} {vars decl} {commands} endmodule
	 * @throws VarNotFoundException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleVarException
	 * @throws IncompatibleActionException
	 * @throws ActionNotFoundException
	 * @throws EffectClassNotFoundException
	 */
	private String buildModule(String moduleName, StateSpace moduleVarSpace,
			Map<FactoredPSO<IAction>, Set<EffectClass>> actionPSOs) throws VarNotFoundException,
			EffectClassNotFoundException, ActionNotFoundException, IncompatibleActionException,
			IncompatibleVarException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException {
		currModuleName = moduleName;

		StringBuilder builder = new StringBuilder();
		builder.append("module ");
		builder.append(moduleName);
		builder.append("\n");
		String varsDecl = mUtilities.buildModuleVarsDecl(moduleVarSpace, mXMDP.getInitialState());
		builder.append(varsDecl);

		if (mThreeParamRewards) {
			builder.append(INDENT);
			String moduleSyncVarDecl = mUtilities.buildModuleSyncVarDecl(moduleName);
			builder.append(moduleSyncVarDecl);
			builder.append("\n");
		}

		builder.append("\n");
		String commands = buildModuleCommands(actionPSOs);
		builder.append(commands);

		if (mThreeParamRewards) {
			builder.append(INDENT);
			String doNothingCommand = mUtilities.buildModuleDoNothingCommand(moduleName);
			builder.append(doNothingCommand);
			builder.append("\n\n");

			builder.append(INDENT);
			String moduleSyncCommand = mUtilities.buildModuleSyncCommand(moduleName);
			builder.append(moduleSyncCommand);
			builder.append("\n");
		}

		builder.append("endmodule");
		return builder.toString();
	}

	/**
	 * 
	 * @param actionPSOs
	 *            : A mapping from each action PSO to (a subset of) its effect classes that are "chained" by other
	 *            effect classes of other action types
	 * @return all commands of a module in the form [actionX] {guard_1} -> {updates_1}; ... [actionZ] {guard_p} ->
	 *         {updates_p};
	 * @throws EffectClassNotFoundException
	 * @throws ActionNotFoundException
	 * @throws IncompatibleActionException
	 * @throws IncompatibleVarException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleDiscriminantClassException
	 * @throws VarNotFoundException
	 */
	private String buildModuleCommands(Map<FactoredPSO<IAction>, Set<EffectClass>> actionPSOs)
			throws EffectClassNotFoundException, ActionNotFoundException, IncompatibleActionException,
			IncompatibleVarException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException,
			VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		for (Entry<FactoredPSO<IAction>, Set<EffectClass>> entry : actionPSOs.entrySet()) {
			FactoredPSO<IAction> actionPSO = entry.getKey();
			Set<EffectClass> chainedEffectClasses = entry.getValue();
			IActionDescription<IAction> actionDesc;
			if (chainedEffectClasses.size() > 1) {
				actionDesc = merge(actionPSO, chainedEffectClasses);
			} else {
				EffectClass effectClass = chainedEffectClasses.iterator().next();
				actionDesc = actionPSO.getActionDescription(effectClass);
			}
			String actionDefName = actionPSO.getActionDefinition().getName();
			String commands = buildModuleCommands(actionDesc);
			builder.append(INDENT);
			builder.append("// ");
			builder.append(actionDefName);
			builder.append("\n");
			builder.append(commands);
			builder.append("\n");
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param actionPSO
	 * @param chainedEffectClasses
	 * @return An action description of a merged effect class of chainedEffectClasses
	 * @throws EffectClassNotFoundException
	 * @throws ActionNotFoundException
	 * @throws IncompatibleActionException
	 * @throws IncompatibleVarException
	 * @throws IncompatibleEffectClassException
	 * @throws IncompatibleDiscriminantClassException
	 */
	private IActionDescription<IAction> merge(FactoredPSO<IAction> actionPSO, Set<EffectClass> chainedEffectClasses)
			throws EffectClassNotFoundException, ActionNotFoundException, IncompatibleActionException,
			IncompatibleVarException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException {
		ActionDefinition<IAction> actionDef = actionPSO.getActionDefinition();

		ActionDescription<IAction> mergedActionDesc = new ActionDescription<>(actionDef);
		Set<ProbabilisticTransition<IAction>> mergedProbTransitions = new HashSet<>();

		for (IAction action : actionDef.getActions()) {
			for (EffectClass effectClass : chainedEffectClasses) {
				IActionDescription<IAction> actionDesc = actionPSO.getActionDescription(effectClass);
				Set<ProbabilisticTransition<IAction>> probTransitions = actionDesc.getProbabilisticTransitions(action);
				mergedProbTransitions = merge(action, mergedProbTransitions, probTransitions);
			}
			mergedActionDesc.putAll(mergedProbTransitions);
		}
		return mergedActionDesc;
	}

	/**
	 * 
	 * @param action
	 *            : Action of all probabilistic transitions in probTransitionsA and probTransitionsB
	 * @param probTransitionsA
	 *            : All probabilistic transitions of effect class A
	 * @param probTransitionsB
	 *            : All probabilistic transitions of effect class B
	 * @return All probabilistic transitions of a merged effect class A and B
	 * @throws IncompatibleActionException
	 * @throws IncompatibleVarException
	 * @throws IncompatibleEffectClassException
	 */
	private Set<ProbabilisticTransition<IAction>> merge(IAction action,
			Set<ProbabilisticTransition<IAction>> probTransitionsA,
			Set<ProbabilisticTransition<IAction>> probTransitionsB)
			throws IncompatibleActionException, IncompatibleVarException, IncompatibleEffectClassException {
		Set<ProbabilisticTransition<IAction>> mergedProbTransitions = new HashSet<>();

		for (ProbabilisticTransition<IAction> probTransA : probTransitionsA) {
			if (!action.equals(probTransA.getAction())) {
				throw new IncompatibleActionException(probTransA.getAction());
			}

			Discriminant discrA = probTransA.getDiscriminant();
			ProbabilisticEffect probEffectA = probTransA.getProbabilisticEffect();

			for (ProbabilisticTransition<IAction> probTransB : probTransitionsB) {
				if (!action.equals(probTransB.getAction())) {
					throw new IncompatibleActionException(probTransB.getAction());
				}

				Discriminant discrB = probTransB.getDiscriminant();
				ProbabilisticEffect probEffectB = probTransB.getProbabilisticEffect();

				DiscriminantClass aggrDiscrClass = new DiscriminantClass();
				aggrDiscrClass.addAll(discrA.getDiscriminantClass());
				aggrDiscrClass.addAll(discrB.getDiscriminantClass());
				Discriminant aggrDiscr = new Discriminant(aggrDiscrClass);
				aggrDiscr.addAll(discrA);
				aggrDiscr.addAll(discrB);

				EffectClass aggrEffectClass = new EffectClass();
				aggrEffectClass.addAll(probEffectA.getEffectClass());
				aggrEffectClass.addAll(probEffectB.getEffectClass());
				ProbabilisticEffect aggrProbEffect = new ProbabilisticEffect(aggrEffectClass);
				aggrProbEffect.putAll(probEffectA, probEffectB);

				ProbabilisticTransition<IAction> mergedProbTrans = new ProbabilisticTransition<>(aggrProbEffect,
						aggrDiscr, action);
				mergedProbTransitions.add(mergedProbTrans);
			}
		}
		return mergedProbTransitions;
	}

	/**
	 * 
	 * @param actionDescription
	 * @return commands for updating a particular effect class
	 * @throws ActionNotFoundException
	 * @throws VarNotFoundException
	 */
	private String buildModuleCommands(IActionDescription<IAction> actionDescription)
			throws ActionNotFoundException, VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		ActionDefinition<IAction> actionDef = actionDescription.getActionDefinition();
		for (IAction action : actionDef.getActions()) {
			Set<ProbabilisticTransition<IAction>> probTransitions = actionDescription
					.getProbabilisticTransitions(action);
			for (ProbabilisticTransition<IAction> probTrans : probTransitions) {
				Discriminant discriminant = probTrans.getDiscriminant();
				ProbabilisticEffect probEffect = probTrans.getProbabilisticEffect();
				String command = mUtilities.buildModuleCommand(action, discriminant, probEffect);
				builder.append(INDENT);
				builder.append(command);
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	private String sanitizeNameString(String name) {
		return name.replace(".", "_");
	}

}
