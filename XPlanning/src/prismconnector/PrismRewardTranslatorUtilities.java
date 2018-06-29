package prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import exceptions.XMDPException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.EffectClass;
import mdp.FactoredPSO;
import mdp.Precondition;
import mdp.TransitionFunction;
import metrics.IQFunction;
import metrics.Transition;
import metrics.TransitionDefinition;
import objectives.AttributeCostFunction;
import objectives.IAdditiveCostFunction;

public class PrismRewardTranslatorUtilities {

	private static final double ARTIFICIAL_REWARD_VALUE = 0.01;

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
	String buildRewardStructures(TransitionFunction transFunction, Set<IQFunction> qFunctions) throws XMDPException {
		StringBuilder builder = new StringBuilder();
		builder.append("// Quality-Attribute Functions\n\n");
		boolean first = true;
		for (IQFunction qFunction : qFunctions) {
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

		builder.append("rewards \"");
		builder.append(objectiveFunction.getName());
		builder.append("\"\n");

		for (AttributeCostFunction<IQFunction> attrCostFunction : objectiveFunction) {
			IQFunction qFunction = attrCostFunction.getQFunction();
			double scalingConst = objectiveFunction.getScalingConstant(attrCostFunction);

			TransitionDefinition transDef = qFunction.getTransitionDefinition();
			FactoredPSO<IAction> actionPSO = transFunction.getActionPSO(transDef.getActionDef());
			TransitionEvaluator evaluator = new TransitionEvaluator() {

				@Override
				public double evaluate(Transition transition)
						throws VarNotFoundException, AttributeNameNotFoundException {
					double attrCost = attrCostFunction.getCost(transition);
					return scalingConst * attrCost;
				}
			};

			String rewardItems = buildRewardItems(objectiveFunction.getName(), transDef, actionPSO, evaluator);
			builder.append(rewardItems);
		}

		String artificialReward = buildArtificialRewardItem(ARTIFICIAL_REWARD_VALUE);
		builder.append(artificialReward);
		builder.append("\n");
		builder.append("endrewards");
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
	String buildRewardStructure(TransitionFunction transFunction, IQFunction qFunction) throws XMDPException {
		String rewardName = qFunction.getName();
		TransitionDefinition transDef = qFunction.getTransitionDefinition();
		FactoredPSO<IAction> actionPSO = transFunction.getActionPSO(transDef.getActionDef());
		TransitionEvaluator evaluator = new TransitionEvaluator() {

			@Override
			public double evaluate(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException {
				return qFunction.getValue(transition);
			}
		};

		StringBuilder builder = new StringBuilder();

		if (mEncodings.isThreeParamRewards() && mPrismRewardType == PrismRewardType.STATE_REWARD) {
			String computeQAFormula = buildComputeRewardFormula(rewardName);
			builder.append(computeQAFormula);
			builder.append("\n\n");
		}

		builder.append("rewards \"");
		builder.append(rewardName);
		builder.append("\"\n");
		String rewardItems = buildRewardItems(rewardName, transDef, actionPSO, evaluator);
		builder.append(rewardItems);
		builder.append("endrewards");
		return builder.toString();
	}

	String buildComputeRewardFormula(String rewardName) {
		return "formula compute_" + rewardName + " = !readyToCopy;";
	}

	/**
	 * Build reward items for a given QA function. The reward values may represent either: (1)~a scaled cost of the QA
	 * of each transition, or (2)~an actual value of the QA of each transition, depending on the given evaluator.
	 * 
	 * @param rewardName
	 *            : Name of the reward structure
	 * @param transDef
	 *            : Transition definition
	 * @param actionPSO
	 *            : PSO of the corresponding action type
	 * @param evaluator
	 *            : A function that assigns a value to a transition
	 * @return compute_{QA name} & action={encoded action value} & {srcVarName}={value} ... & {destVarName}={value} ...
	 *         : {transition value}; ...
	 * @throws XMDPException
	 */
	String buildRewardItems(String rewardName, TransitionDefinition transDef, FactoredPSO<IAction> actionPSO,
			TransitionEvaluator evaluator) throws XMDPException {
		Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs = transDef.getSrcStateVarDefs();
		Set<StateVarDefinition<IStateVarValue>> destStateVarDefs = transDef.getDestStateVarDefs();
		ActionDefinition<IAction> actionDef = transDef.getActionDef();

		StringBuilder builder = new StringBuilder();

		for (IAction action : actionDef.getActions()) {
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

					Transition transition = new Transition(action, srcVars, destVars);
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
			FactoredPSO<IAction> actionPSO) {
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

	private Set<Set<StateVar<IStateVarValue>>> getApplicableSrcValuesCombinations(FactoredPSO<IAction> actionPSO,
			IAction action, Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs) throws ActionNotFoundException {
		Precondition<IAction> precondition = actionPSO.getPrecondition(action);
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> srcVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> srcVarDef : srcStateVarDefs) {
			Set<IStateVarValue> applicableVals = precondition.getApplicableValues(action, srcVarDef);
			srcVarValues.put(srcVarDef, applicableVals);
		}
		return getCombinations(srcVarValues);
	}

	private Set<Set<StateVar<IStateVarValue>>> getPossibleDestValuesCombination(FactoredPSO<IAction> actionPSO,
			IAction action, Set<StateVarDefinition<IStateVarValue>> destStateVarDefs,
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
			Set<StateVar<IStateVarValue>> srcVars, FactoredPSO<IAction> actionPSO)
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

	interface TransitionEvaluator {
		double evaluate(Transition transition) throws VarNotFoundException, AttributeNameNotFoundException;
	}

}
