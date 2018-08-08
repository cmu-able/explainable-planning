package prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import language.exceptions.ActionNotFoundException;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.IncompatibleVarException;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.EffectClass;
import language.mdp.FactoredPSO;
import language.mdp.Precondition;
import language.mdp.TransitionFunction;
import language.metrics.IEvent;
import language.metrics.IQFunction;
import language.metrics.ITransitionStructure;
import language.metrics.Transition;
import language.objectives.AttributeCostFunction;
import language.objectives.IAdditiveCostFunction;
import language.qfactors.ActionDefinition;
import language.qfactors.IAction;
import language.qfactors.IStateVarValue;
import language.qfactors.StateVar;
import language.qfactors.StateVarDefinition;

public class PrismRewardTranslatorUtilities {

	private static final double ARTIFICIAL_REWARD_VALUE = 0.01;
	private static final String BEGIN_REWARDS = "rewards \"%s\"";
	private static final String END_REWARDS = "endrewards";

	private ValueEncodingScheme mEncodings;
	private PrismRewardType mPrismRewardType;

	public PrismRewardTranslatorUtilities(ValueEncodingScheme encodings, PrismRewardType prismRewardType) {
		mEncodings = encodings;
		mPrismRewardType = prismRewardType;
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
		StringBuilder builder = new StringBuilder();
		builder.append("// Quality-Attribute Functions\n\n");
		boolean first = true;
		for (IQFunction<?, ?> qFunction : qFunctions) {
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

			// Record the order of which the reward structures representing the QA functions are written to the model
			// To keep track of the reward-structure-index corresponding to each QA function
			mEncodings.appendQFunction(qFunction);
		}
		return builder.toString();
	}

	/**
	 * Build a state-based reward structure for a given objective function. This can be the cost function of MDP or an
	 * arbitrary objective function.
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
		String rewardName = event.getName() + "_count";
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
			Integer encodedActionValue = mEncodings.getEncodedIntValue(action);

			Set<Set<StateVar<IStateVarValue>>> srcCombinations = getApplicableSrcValuesCombinations(actionPSO, action,
					srcStateVarDefs);
			for (Set<StateVar<IStateVarValue>> srcVars : srcCombinations) {
				// Separate variables of the source state into changed and unchanged variables
				// This is to ease PRISM MDP model generation
				Set<StateVar<IStateVarValue>> unchangedSrcVars = getUnchangedStateVars(srcVars, actionPSO);
				Set<StateVar<IStateVarValue>> changedSrcVars = new HashSet<>(srcVars);
				changedSrcVars.removeAll(unchangedSrcVars);

				String changedSrcPartialGuard = buildPartialRewardGuard(changedSrcVars,
						PrismTranslatorUtilities.SRC_SUFFIX);
				String unchangedSrcPartialGuard = buildPartialRewardGuard(unchangedSrcVars);

				Set<Set<StateVar<IStateVarValue>>> destCombinations = getPossibleDestValuesCombination(actionPSO,
						action, destStateVarDefs, srcVars);
				for (Set<StateVar<IStateVarValue>> destVars : destCombinations) {
					String destPartialGuard = buildPartialRewardGuard(destVars);

					Transition<E, T> transition = new Transition<>(transStructure, action, srcVars, destVars);
					double value = evaluator.evaluate(transition);

					builder.append(PrismTranslatorUtilities.INDENT);

					if (mPrismRewardType == PrismRewardType.STATE_REWARD) {
						builder.append("compute_");
						builder.append(rewardName);
						builder.append(" & ");
					} else if (mPrismRewardType == PrismRewardType.TRANSITION_REWARD) {
						builder.append("[compute] ");
					}

					builder.append("action=");
					builder.append(encodedActionValue);
					builder.append(" & ");
					builder.append(changedSrcPartialGuard);
					builder.append(" & ");
					builder.append(unchangedSrcPartialGuard);
					builder.append(" & ");
					builder.append(destPartialGuard);
					builder.append(" : ");
					builder.append(value);
					builder.append(";");
					builder.append("\n");
				}
			}
		}
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
		return PrismTranslatorUtilities.INDENT + synchStr + " : " + value + ";";
	}

	/**
	 * 
	 * @param stateVars
	 * @return {varName_1}={encoded int value} & ... & {varName_m}={encoded int value}
	 * @throws VarNotFoundException
	 */
	String buildPartialRewardGuard(Set<StateVar<IStateVarValue>> stateVars) throws VarNotFoundException {
		return buildPartialRewardGuard(stateVars, "");
	}

	/**
	 * 
	 * @param stateVars
	 * @param nameSuffix
	 * @return {varName_1{Suffix}}={encoded int value} & ... & {varName_m{Suffix}}={encoded int value}
	 * @throws VarNotFoundException
	 */
	String buildPartialRewardGuard(Set<StateVar<IStateVarValue>> stateVars, String nameSuffix)
			throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (StateVar<IStateVarValue> var : stateVars) {
			String varName = var.getName();
			Integer encodedValue = mEncodings.getEncodedIntValue(var.getDefinition(), var.getValue());
			if (!first) {
				builder.append(" & ");
			} else {
				first = false;
			}
			builder.append(varName);
			builder.append(nameSuffix);
			builder.append("=");
			builder.append(encodedValue);
		}
		return builder.toString();
	}

	/**
	 * 
	 * @param srcVars
	 * @param actionPSO
	 * @return State variables in srcVars that are not affected by the given action
	 */
	private Set<StateVar<IStateVarValue>> getUnchangedStateVars(Set<StateVar<IStateVarValue>> srcVars,
			FactoredPSO<? extends IAction> actionPSO) {
		Set<StateVar<IStateVarValue>> unchangedVars = new HashSet<>();
		for (StateVar<IStateVarValue> srcVar : srcVars) {
			Set<EffectClass> effectClasses = actionPSO.getIndependentEffectClasses();
			for (EffectClass effectClass : effectClasses) {
				if (!effectClass.contains(srcVar.getDefinition())) {
					unchangedVars.add(srcVar);
				}
			}
		}
		return unchangedVars;
	}

	private <E extends IAction> Set<Set<StateVar<IStateVarValue>>> getApplicableSrcValuesCombinations(
			FactoredPSO<E> actionPSO, E action, Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs)
			throws ActionNotFoundException {
		Precondition<E> precondition = actionPSO.getPrecondition();
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> srcVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> srcVarDef : srcStateVarDefs) {
			Set<IStateVarValue> applicableVals = precondition.getApplicableValues(action, srcVarDef);
			srcVarValues.put(srcVarDef, applicableVals);
		}
		return getCombinations(srcVarValues);
	}

	private <E extends IAction> Set<Set<StateVar<IStateVarValue>>> getPossibleDestValuesCombination(
			FactoredPSO<E> actionPSO, E action, Set<StateVarDefinition<IStateVarValue>> destStateVarDefs,
			Set<StateVar<IStateVarValue>> srcVars) throws XMDPException {
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> destVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> destVarDef : destStateVarDefs) {
			Discriminant discriminant = getDiscriminant(destVarDef, srcVars, actionPSO);
			Set<IStateVarValue> possibleDestVals = actionPSO.getPossibleImpact(destVarDef, discriminant, action);
			destVarValues.put(destVarDef, possibleDestVals);
		}
		return getCombinations(destVarValues);
	}

	private Discriminant getDiscriminant(StateVarDefinition<IStateVarValue> destVarDef,
			Set<StateVar<IStateVarValue>> srcVars, FactoredPSO<? extends IAction> actionPSO)
			throws IncompatibleVarException, VarNotFoundException {
		DiscriminantClass discrClass = actionPSO.getDiscriminantClass(destVarDef);
		Discriminant discriminant = new Discriminant(discrClass);
		for (StateVar<IStateVarValue> var : srcVars) {
			if (discrClass.contains(var.getDefinition())) {
				discriminant.add(var);
			}
		}
		return discriminant;
	}

	private Set<Set<StateVar<IStateVarValue>>> getCombinations(
			Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> varValues) {
		Set<Set<StateVar<IStateVarValue>>> combinations = new HashSet<>();
		Set<StateVar<IStateVarValue>> emptyCombination = new HashSet<>();
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

		// Recursive case: >1 variables
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> partialVarValues = new HashMap<>(varValues);
		partialVarValues.remove(varDef);
		Set<Set<StateVar<IStateVarValue>>> partialCombinations = getCombinations(partialVarValues);
		return getCombinationsHelper(varDef, values, partialCombinations);
	}

	private Set<Set<StateVar<IStateVarValue>>> getCombinationsHelper(StateVarDefinition<IStateVarValue> varDef,
			Set<IStateVarValue> values, Set<Set<StateVar<IStateVarValue>>> partialCombinations) {
		Set<Set<StateVar<IStateVarValue>>> newCombinations = new HashSet<>();
		for (IStateVarValue value : values) {
			StateVar<IStateVarValue> newVar = varDef.getStateVar(value);
			for (Set<StateVar<IStateVarValue>> prevCombination : partialCombinations) {
				Set<StateVar<IStateVarValue>> newCombination = new HashSet<>();
				newCombination.addAll(prevCombination);
				newCombination.add(newVar);
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
