package solver.prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import language.exceptions.ActionNotFoundException;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.EffectClass;
import language.mdp.FactoredPSO;
import language.mdp.Precondition;
import language.mdp.StateVarTuple;
import language.mdp.TransitionFunction;
import language.metrics.IEvent;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;
import language.metrics.Transition;
import language.objectives.AttributeCostFunction;
import language.objectives.IAdditiveCostFunction;
import language.qfactors.ActionDefinition;
import language.qfactors.IAction;
import language.qfactors.IStateVarBoolean;
import language.qfactors.IStateVarInt;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;
import language.qfactors.StateVarDefinition;

public class PrismRewardTranslatorHelper {

	private static final double ARTIFICIAL_REWARD_VALUE = 1.0E-5;
	private static final String BEGIN_REWARDS = "rewards \"%s\"";
	private static final String END_REWARDS = "endrewards";

	private ValueEncodingScheme mEncodings;
	private PrismRewardType mPrismRewardType;

	public PrismRewardTranslatorHelper(ValueEncodingScheme encodings, PrismRewardType prismRewardType) {
		mEncodings = encodings;
		mPrismRewardType = prismRewardType;
	}

	void setPrismRewardType(PrismRewardType prismRewardType) {
		mPrismRewardType = prismRewardType;
	}

	PrismRewardType getPrismRewardType() {
		return mPrismRewardType;
	}

	/**
	 * Build a list of state-based reward structures for a given set of QA functions.
	 * 
	 * @param transFunction
	 *            : Transition function of MDP
	 * @param qFunctions
	 *            : QA functions
	 * @return Reward structures for the QA functions
	 * @throws XMDPException
	 */
	String buildRewardStructures(TransitionFunction transFunction,
			Iterable<IQFunction<IAction, ITransitionStructure<IAction>>> qFunctions) throws XMDPException {
		// Assume that the input QFunctions are all of the QFunctions in XMDP

		// This is to ensure that: the order of which the reward structures representing the QA functions are written to
		// the model correspond to the predefined reward-structure-index of each QA function
		List<IQFunction<IAction, ITransitionStructure<IAction>>> orderedQFunctions = mEncodings
				.getQFunctionEncodingScheme().getOrderedQFunctions();

		StringBuilder builder = new StringBuilder();
		builder.append("// Quality-Attribute Functions\n\n");
		boolean first = true;
		for (IQFunction<?, ?> qFunction : orderedQFunctions) {
			if (!first) {
				builder.append("\n\n");
			} else {
				first = false;
			}
			builder.append("// ");
			builder.append(qFunction.getName());
			builder.append("\n\n");
			String rewards = buildRewardStructure(transFunction, qFunction);
			builder.append(rewards);
		}
		return builder.toString();
	}

	/**
	 * Build a state-based reward structure for a given objective function. This can be the cost function of MDP or an
	 * arbitrary objective function.
	 * 
	 * The objective function must be the first reward structure in any PRISM MDP translation.
	 * 
	 * @param transFunction
	 *            : Transition function of MDP
	 * @param objectiveFunction
	 *            : Objective function that this reward structure represents
	 * @return formula compute_{objective name} = !readyToCopy; rewards "{objective name}" ... endrewards
	 * @throws XMDPException
	 */
	String buildRewardStructure(TransitionFunction transFunction, IAdditiveCostFunction objectiveFunction)
			throws XMDPException {
		StringBuilder builder = new StringBuilder();

		if (mEncodings.isThreeParamRewards() && mPrismRewardType == PrismRewardType.STATE_REWARD) {
			String computeCostFormula = buildComputeRewardFormula(objectiveFunction.getName());
			builder.append(computeCostFormula);
			builder.append("\n\n");
		}

		builder.append(String.format(BEGIN_REWARDS, objectiveFunction.getName()));
		builder.append("\n");

		Set<IQFunction<IAction, ITransitionStructure<IAction>>> qFunctions = objectiveFunction.getQFunctions();

		for (IQFunction<IAction, ITransitionStructure<IAction>> qFunction : qFunctions) {
			AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> attrCostFunction = objectiveFunction
					.getAttributeCostFunction(qFunction);
			double scalingConst = objectiveFunction.getScalingConstant(attrCostFunction);

			ITransitionStructure<IAction> domain = qFunction.getTransitionStructure();
			FactoredPSO<IAction> actionPSO = transFunction.getActionPSO(domain.getActionDef());
			TransitionEvaluator<IAction, ITransitionStructure<IAction>> evaluator = new TransitionEvaluator<IAction, ITransitionStructure<IAction>>() {

				@Override
				public double evaluate(Transition<IAction, ITransitionStructure<IAction>> transition)
						throws VarNotFoundException, AttributeNameNotFoundException {
					double qValue = qFunction.getValue(transition);
					double attrCost = attrCostFunction.getCost(qValue);
					return scalingConst * attrCost;
				}
			};

			String rewardItems = buildRewardItems(objectiveFunction.getName(), domain, actionPSO, evaluator);
			builder.append(rewardItems);
		}

		String artificialReward = buildArtificialRewardItem(ARTIFICIAL_REWARD_VALUE);
		builder.append(artificialReward);
		builder.append("\n");
		builder.append(END_REWARDS);
		return builder.toString();
	}

	/**
	 * Build a state-based reward structure for a given QA function.
	 * 
	 * @param transFunction
	 *            : Transition function of MDP
	 * @param qFunction
	 *            : QA function
	 * @return formula compute_{QA name} = !readyToCopy; rewards "{QA name}" ... endrewards
	 * @throws XMDPException
	 */
	<E extends IAction, T extends ITransitionStructure<E>> String buildRewardStructure(TransitionFunction transFunction,
			IQFunction<E, T> qFunction) throws XMDPException {
		String rewardName = qFunction.getName();
		T domain = qFunction.getTransitionStructure();
		FactoredPSO<E> actionPSO = transFunction.getActionPSO(domain.getActionDef());
		TransitionEvaluator<E, T> evaluator = new TransitionEvaluator<E, T>() {

			@Override
			public double evaluate(Transition<E, T> transition)
					throws VarNotFoundException, AttributeNameNotFoundException {
				return qFunction.getValue(transition);
			}
		};

		StringBuilder builder = new StringBuilder();

		if (mEncodings.isThreeParamRewards() && mPrismRewardType == PrismRewardType.STATE_REWARD) {
			String computeQAFormula = buildComputeRewardFormula(rewardName);
			builder.append(computeQAFormula);
			builder.append("\n\n");
		}

		builder.append(String.format(BEGIN_REWARDS, rewardName));
		builder.append("\n");
		String rewardItems = buildRewardItems(rewardName, domain, actionPSO, evaluator);
		builder.append(rewardItems);
		builder.append(END_REWARDS);
		return builder.toString();
	}

	/**
	 * Build a list of reward structures for counting events.
	 * 
	 * @param transFunction
	 *            : Transition function of MDP
	 * @param events
	 *            : Events to be counted
	 * @return Reward structures for counting the events.
	 * @throws XMDPException
	 */
	String buildRewardStructuresForEventCounts(TransitionFunction transFunction, Set<? extends IEvent<?, ?>> events)
			throws XMDPException {
		StringBuilder builder = new StringBuilder();
		builder.append("// Counters for events\n\n");
		boolean first = true;
		for (IEvent<?, ?> event : events) {
			if (!first) {
				builder.append("\n\n");
			} else {
				first = false;
			}
			builder.append("// ");
			builder.append(event.getName());
			builder.append("\n\n");
			String rewards = buildRewardStructureForEventCount(transFunction, event);
			builder.append(rewards);
		}
		return builder.toString();
	}

	/**
	 * Build a reward structure for counting a particular event.
	 * 
	 * @param transFunction
	 *            : Transition function of MDP
	 * @param event
	 *            : Event to be counted
	 * @return Reward structure for counting the event.
	 * @throws XMDPException
	 */
	<E extends IAction, T extends ITransitionStructure<E>> String buildRewardStructureForEventCount(
			TransitionFunction transFunction, IEvent<E, T> event) throws XMDPException {
		String sanitizedEventName = PrismTranslatorUtils.sanitizeNameString(event.getName());
		String rewardName = sanitizedEventName + "_count";
		T eventStructure = event.getTransitionStructure();
		FactoredPSO<E> actionPSO = transFunction.getActionPSO(eventStructure.getActionDef());
		TransitionEvaluator<E, T> evaluator = new TransitionEvaluator<E, T>() {

			@Override
			public double evaluate(Transition<E, T> transition)
					throws VarNotFoundException, AttributeNameNotFoundException {
				return event.hasEventOccurred(transition) ? 1 : 0;
			}
		};

		StringBuilder builder = new StringBuilder();

		if (mEncodings.isThreeParamRewards() && mPrismRewardType == PrismRewardType.STATE_REWARD) {
			String computeEventCountFormula = buildComputeRewardFormula(rewardName);
			builder.append(computeEventCountFormula);
			builder.append("\n\n");
		}

		builder.append(String.format(BEGIN_REWARDS, rewardName));
		builder.append("\n");
		String rewardItems = buildRewardItems(rewardName, eventStructure, actionPSO, evaluator);
		builder.append(rewardItems);
		builder.append(END_REWARDS);
		return builder.toString();
	}

	String buildComputeRewardFormula(String rewardName) {
		return "formula compute_" + rewardName + " = !readyToCopy;";
	}

	/**
	 * Build reward items for a given evaluator. The reward values may represent either: (1)~a scaled cost of the QA of
	 * each transition, (2)~an actual value of the QA of each transition, or (3)~occurrence of a particular event,
	 * depending on the given evaluator.
	 * 
	 * @param rewardName
	 *            : Name of the reward structure
	 * @param transStructure
	 *            : Transition structure -- this may be the domain of a QA function
	 * @param actionPSO
	 *            : PSO of the corresponding action type
	 * @param evaluator
	 *            : A function that assigns a value to a transition
	 * @return compute_{QA name} & action={encoded action value} & {srcVarName}={value} ... & {destVarName}={value} ...
	 *         : {transition value}; ...
	 * @throws XMDPException
	 */
	<E extends IAction, T extends ITransitionStructure<E>> String buildRewardItems(String rewardName, T transStructure,
			FactoredPSO<E> actionPSO, TransitionEvaluator<E, T> evaluator) throws XMDPException {
		Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs = transStructure.getSrcStateVarDefs();
		Set<StateVarDefinition<IStateVarValue>> destStateVarDefs = transStructure.getDestStateVarDefs();
		ActionDefinition<E> actionDef = transStructure.getActionDef();

		StringBuilder builder = new StringBuilder();

		for (E action : actionDef.getActions()) {
			Set<StateVarTuple> srcCombinations = getApplicableSrcValuesCombinations(srcStateVarDefs, action, actionPSO);

			for (StateVarTuple srcVars : srcCombinations) {
				Set<StateVarTuple> destCombinations = getPossibleDestValuesCombinations(destStateVarDefs, srcVars,
						action, actionPSO);

				for (StateVarTuple destVars : destCombinations) {
					String rewardItem = buildRewardItem(rewardName, transStructure, action, srcVars, destVars,
							actionPSO, evaluator);
					builder.append(rewardItem);
					builder.append("\n");
				}
			}
		}
		return builder.toString();
	}

	/**
	 * Build a reward item for a given evaluator.
	 * 
	 * @param rewardName
	 * @param transStructure
	 * @param action
	 * @param srcCombinations
	 *            : If there is no source variable definition, then this is a singleton set of an empty
	 *            {@link StateVarTuple}
	 * @param destCombinations:
	 *            If there is no destination variable definition, then this is a singleton set of an empty
	 *            {@link StateVarTuple}
	 * @param actionPSO
	 * @param evaluator
	 * @return compute_{QA name} & action={encoded action value} & {srcVarName}={value} ... & {destVarName}={value} ...
	 *         : {transition value};
	 * @throws ActionNotFoundException
	 */
	<E extends IAction, T extends ITransitionStructure<E>> String buildRewardItem(String rewardName, T transStructure,
			E action, StateVarTuple srcVars, StateVarTuple destVars, FactoredPSO<E> actionPSO,
			TransitionEvaluator<E, T> evaluator) throws XMDPException {
		// Encoded int action value
		Integer encodedActionValue = mEncodings.getEncodedIntValue(action);

		// Separate variables of the source state into changed and unchanged variables
		// This is to ease PRISM MDP model generation
		StateVarTuple unchangedSrcVars = getUnchangedStateVars(srcVars, actionPSO);
		StateVarTuple changedSrcVars = getChangedStateVars(srcVars, unchangedSrcVars);

		// Compute value of the transition
		Transition<E, T> transition = new Transition<>(transStructure, action, srcVars, destVars);
		double value = evaluator.evaluate(transition);

		String srcPartialGuard = buildSrcPartialRewardGuard(changedSrcVars, unchangedSrcVars);
		String destPartialGuard = buildDestPartialRewardGuard(destVars);

		StringBuilder builder = new StringBuilder();
		builder.append(PrismTranslatorHelper.INDENT);

		if (mPrismRewardType == PrismRewardType.STATE_REWARD) {
			builder.append("compute_");
			builder.append(rewardName);
			builder.append(" & ");
		} else if (mPrismRewardType == PrismRewardType.TRANSITION_REWARD) {
			builder.append("[compute] ");
		}

		// "action={encoded action value}"
		builder.append("action=");
		builder.append(encodedActionValue);

		// "" or "& {srcVarName}={value} ..."
		builder.append(srcPartialGuard);

		// "" or "& {destVarName}={value} ..."
		builder.append(destPartialGuard);

		// " : {transition value};"
		builder.append(" : ");
		builder.append(value);
		builder.append(";");
		return builder.toString();
	}

	/**
	 * This is to ensure that there is no zero-reward cycle in the MDP. This is because the current version of PRISM 4.4
	 * does not support "constructing a strategy for Rmin in the presence of zero-reward ECs".
	 * 
	 * @param value
	 *            : Artificial reward value assigned to every "compute" transition
	 * @return compute_cost : {value};
	 */
	String buildArtificialRewardItem(double value) {
		String synchStr = mPrismRewardType == PrismRewardType.STATE_REWARD ? "compute_cost" : "[compute] true";
		return PrismTranslatorHelper.INDENT + synchStr + " : " + value + ";";
	}

	/**
	 * 
	 * @param stateVars
	 * @return {varName_1}={encoded int value} & ... & {varName_m}={encoded int value}
	 * @throws VarNotFoundException
	 */
	String buildPartialRewardGuard(StateVarTuple stateVars) throws VarNotFoundException {
		return buildPartialRewardGuard(stateVars, "");
	}

	/**
	 * 
	 * @param stateVars
	 * @param nameSuffix
	 * @return {varName_1{Suffix}}={value OR encoded int value} & ... & {varName_m{Suffix}}={value OR encoded int value}
	 * @throws VarNotFoundException
	 */
	String buildPartialRewardGuard(StateVarTuple stateVars, String nameSuffix) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (StateVar<IStateVarValue> var : stateVars) {
			String varName = var.getName();
			IStateVarValue value = var.getValue();

			if (!first) {
				builder.append(" & ");
			} else {
				first = false;
			}
			builder.append(varName);
			builder.append(nameSuffix);
			builder.append("=");

			if (value instanceof IStateVarInt || value instanceof IStateVarBoolean) {
				builder.append(value);
			} else {
				Integer encodedValue = mEncodings.getEncodedIntValue(var.getDefinition(), value);
				builder.append(encodedValue);
			}
		}
		return builder.toString();
	}

	/**
	 * Returns an empty String if there is no source variable.
	 * 
	 * @param changedSrcVars
	 * @param unchangedSrcVars
	 * @return & {changedSrcVarName}={value} ... & {unchangedSrcVarName}={value} ...
	 * @throws VarNotFoundException
	 */
	private String buildSrcPartialRewardGuard(StateVarTuple changedSrcVars, StateVarTuple unchangedSrcVars)
			throws VarNotFoundException {
		String changedSrcPartialGuard = buildPartialRewardGuard(changedSrcVars, PrismTranslatorHelper.SRC_SUFFIX);
		String unchangedSrcPartialGuard = buildPartialRewardGuard(unchangedSrcVars);

		StringBuilder builder = new StringBuilder();
		if (!changedSrcPartialGuard.isEmpty()) {
			builder.append(" & ");
			builder.append(changedSrcPartialGuard);
		}
		if (!unchangedSrcPartialGuard.isEmpty()) {
			builder.append(" & ");
			builder.append(unchangedSrcPartialGuard);
		}
		return builder.toString();
	}

	/**
	 * Returns an empty String if there is no destination variable.
	 * 
	 * @param destVars
	 * @return & {destVarName}={value} ...
	 * @throws VarNotFoundException
	 */
	private String buildDestPartialRewardGuard(StateVarTuple destVars) throws VarNotFoundException {
		String destPartialGuard = buildPartialRewardGuard(destVars);

		StringBuilder builder = new StringBuilder();
		if (!destPartialGuard.isEmpty()) {
			builder.append(" & ");
			builder.append(destPartialGuard);
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param srcVars
	 * @param actionPSO
	 * @return State variables in srcVars that are not affected by the given action
	 */
	private StateVarTuple getUnchangedStateVars(StateVarTuple srcVars, FactoredPSO<? extends IAction> actionPSO) {
		StateVarTuple unchangedVars = new StateVarTuple();
		for (StateVar<IStateVarValue> srcVar : srcVars) {
			boolean affected = false;
			Set<EffectClass> effectClasses = actionPSO.getIndependentEffectClasses();
			for (EffectClass effectClass : effectClasses) {
				if (effectClass.contains(srcVar.getDefinition())) {
					affected = true;
					break;
				}
			}
			if (!affected) {
				unchangedVars.addStateVar(srcVar);
			}
		}
		return unchangedVars;
	}

	private StateVarTuple getChangedStateVars(StateVarTuple srcVars, StateVarTuple unchangedSrcVars) {
		StateVarTuple changedVars = new StateVarTuple();
		for (StateVar<IStateVarValue> srcVar : srcVars) {
			if (!unchangedSrcVars.contains(srcVar.getDefinition())) {
				changedVars.addStateVar(srcVar);
			}
		}
		return changedVars;
	}

	/**
	 * Generate all applicable value combinations of a given set of source variables. The applicable combinations are
	 * determined by action precondition.
	 * 
	 * If there is no source variable definition, then this method returns a singleton set of an empty
	 * {@link StateVarTuple}.
	 * 
	 * @param srcStateVarDefs
	 *            : Source variable definitions
	 * @param action
	 *            : Action
	 * @param actionPSO
	 * @return All applicable source value combinations
	 * @throws ActionNotFoundException
	 */
	private <E extends IAction> Set<StateVarTuple> getApplicableSrcValuesCombinations(
			Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs, E action, FactoredPSO<E> actionPSO)
			throws ActionNotFoundException {
		Precondition<E> precondition = actionPSO.getPrecondition();
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> srcVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> srcVarDef : srcStateVarDefs) {
			Set<IStateVarValue> applicableVals = precondition.getApplicableValues(action, srcVarDef);
			srcVarValues.put(srcVarDef, applicableVals);
		}
		return getCombinations(srcVarValues);
	}

	/**
	 * Generate all possible value combinations of a given set of destination variables. The possible combinations are
	 * determined by source variables (if any) and action precondition.
	 * 
	 * If there is no destination variable definition, then this method returns a singleton set of an empty
	 * {@link StateVarTuple}.
	 * 
	 * @param destStateVarDefs
	 *            : Destination variable definitions
	 * @param srcVars
	 *            : Source variables
	 * @param action
	 *            : Action
	 * @param actionPSO
	 * @return All possible destination value combinations
	 * @throws XMDPException
	 */
	private <E extends IAction> Set<StateVarTuple> getPossibleDestValuesCombinations(
			Set<StateVarDefinition<IStateVarValue>> destStateVarDefs, StateVarTuple srcVars, E action,
			FactoredPSO<E> actionPSO) throws XMDPException {
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> destVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> destVarDef : destStateVarDefs) {
			Set<Discriminant> applicableDiscriminants = getApplicableDiscriminants(destVarDef, srcVars, actionPSO,
					action);
			Set<IStateVarValue> possibleDestVals = new HashSet<>();
			for (Discriminant discriminant : applicableDiscriminants) {
				Set<IStateVarValue> possibleDestValsFromDiscr = actionPSO.getPossibleImpact(destVarDef, discriminant,
						action);
				possibleDestVals.addAll(possibleDestValsFromDiscr);
			}
			destVarValues.put(destVarDef, possibleDestVals);
		}
		return getCombinations(destVarValues);
	}

	/**
	 * Get all applicable discriminants of a given destination variable definition and source variables. That is, get
	 * all possible discriminants of the destination variable, that contain the source variables and satisfy the
	 * action's precondition.
	 * 
	 * This method may return more than 1 discriminant if the discriminant class has other variables than those in the
	 * source variables.
	 * 
	 * @param destVarDef
	 *            : Destination variable definition
	 * @param srcVars
	 *            : Source variables
	 * @param actionPSO
	 *            : Action PSO
	 * @return All applicable discriminants
	 * @throws XMDPException
	 */
	private <E extends IAction> Set<Discriminant> getApplicableDiscriminants(
			StateVarDefinition<IStateVarValue> destVarDef, StateVarTuple srcVars, FactoredPSO<E> actionPSO, E action)
			throws XMDPException {
		DiscriminantClass discrClass = actionPSO.getDiscriminantClass(destVarDef);
		Discriminant boundedDiscriminant = new Discriminant(discrClass);

		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> freeDiscrVars = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> varDef : discrClass) {
			if (srcVars.contains(varDef)) {
				// This discriminant variable is in srcVars
				// Add this variable to (each) discriminant generated
				IStateVarValue value = srcVars.getStateVarValue(IStateVarValue.class, varDef);
				StateVar<IStateVarValue> srcVar = varDef.getStateVar(value);
				boundedDiscriminant.add(srcVar);
			} else {
				// Discriminant class has a variable that is not in srcVars
				// Get all applicable values of that variable -- to find all applicable discriminants
				Precondition<E> precond = actionPSO.getPrecondition();
				Set<IStateVarValue> applicableValues = precond.getApplicableValues(action, varDef);
				freeDiscrVars.put(varDef, applicableValues);
			}
		}

		if (freeDiscrVars.isEmpty()) {
			Set<Discriminant> singleton = new HashSet<>();
			singleton.add(boundedDiscriminant);
			return singleton;
		}

		// Get all value combinations of the "free" discriminant variables
		Set<StateVarTuple> subDiscriminants = getCombinations(freeDiscrVars);

		// Generate all applicable discriminants by combining the "bounded" discriminant with all combinations of the
		// "free" discriminant variables
		Set<Discriminant> discriminants = new HashSet<>();
		for (StateVarTuple subDiscriminant : subDiscriminants) {
			Discriminant fullDiscriminant = new Discriminant(discrClass);

			// Add discriminant variables that are in srcVars
			fullDiscriminant.addAll(boundedDiscriminant);

			// Add other discriminant variables that are not in srcVars
			for (StateVar<IStateVarValue> var : subDiscriminant) {
				fullDiscriminant.add(var);
			}

			discriminants.add(fullDiscriminant);
		}
		return discriminants;
	}

	/**
	 * Generate a set of all value combinations of a given set of state variable definitions and their allowable values.
	 * 
	 * If there is no variable definition, then this method returns a set of an empty {@link StateVarTuple}.
	 * 
	 * @param varValues
	 * @return All value combinations of a given set of state variable definitions and their allowable values.
	 */
	private Set<StateVarTuple> getCombinations(Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> varValues) {
		Set<StateVarTuple> combinations = new HashSet<>();
		StateVarTuple emptyCombination = new StateVarTuple();
		combinations.add(emptyCombination);

		// Base case: no variable
		if (varValues.isEmpty()) {
			return combinations;
		}

		StateVarDefinition<IStateVarValue> varDef = varValues.keySet().iterator().next();
		Set<IStateVarValue> values = varValues.get(varDef);

		// Base case: 1 variable
		if (varValues.size() == 1) {
			return getCombinationsHelper(varDef, values, combinations);
		}

		// Recursive case: > 1 variables
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> partialVarValues = new HashMap<>(varValues);
		partialVarValues.remove(varDef);
		Set<StateVarTuple> partialCombinations = getCombinations(partialVarValues);
		return getCombinationsHelper(varDef, values, partialCombinations);
	}

	private Set<StateVarTuple> getCombinationsHelper(StateVarDefinition<IStateVarValue> varDef,
			Set<IStateVarValue> values, Set<StateVarTuple> partialCombinations) {
		Set<StateVarTuple> newCombinations = new HashSet<>();
		for (IStateVarValue value : values) {
			StateVar<IStateVarValue> newVar = varDef.getStateVar(value);
			for (StateVarTuple prevCombination : partialCombinations) {
				StateVarTuple newCombination = new StateVarTuple();
				newCombination.addStateVarTuple(prevCombination);
				newCombination.addStateVar(newVar);
				newCombinations.add(newCombination);
			}
		}
		return newCombinations;
	}

	/**
	 * {@link TransitionEvaluator} is an interface to a function that evaluates a real-value of a transition. This
	 * function can calculate a QA value of a transition, or calculate a scaled cost of a particular QA of a transition.
	 * 
	 * @author rsukkerd
	 *
	 * @param <E>
	 * @param <T>
	 */
	interface TransitionEvaluator<E extends IAction, T extends ITransitionStructure<E>> {
		double evaluate(Transition<E, T> transition) throws VarNotFoundException, AttributeNameNotFoundException;
	}

}
