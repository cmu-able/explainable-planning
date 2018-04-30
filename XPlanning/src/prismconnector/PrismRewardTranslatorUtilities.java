package prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import exceptions.ActionDefinitionNotFoundException;
import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.IncompatibleVarException;
import exceptions.VarNotFoundException;
import factors.ActionDefinition;
import factors.IAction;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.FactoredPSO;
import mdp.Precondition;
import mdp.State;
import mdp.TransitionFunction;
import metrics.IQFunction;
import metrics.Transition;
import metrics.TransitionDefinition;
import preferences.AttributeCostFunction;
import preferences.CostFunction;

public class PrismRewardTranslatorUtilities {

	static final String COST_STRUCTURE_NAME = "cost";

	private ValueEncodingScheme mEncodings;

	public PrismRewardTranslatorUtilities(ValueEncodingScheme encodings) {
		mEncodings = encodings;
	}

	public ValueEncodingScheme getValueEncodingScheme() {
		return mEncodings;
	}

	/**
	 * 
	 * @param transFunction
	 * @param qFunctions
	 * @param costFunction
	 * @return formula computeCost = !readyToCopy; rewards "cost" ... endrewards
	 * @throws VarNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws ActionNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 */
	String buildRewards(TransitionFunction transFunction, Set<IQFunction> qFunctions, CostFunction costFunction)
			throws VarNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException, ActionNotFoundException, ActionDefinitionNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("formula computeCost = !readyToCopy;");
		builder.append("\n\n");
		builder.append("rewards \"cost\"");
		builder.append("\n");
		for (IQFunction qFunction : qFunctions) {
			AttributeCostFunction<IQFunction> attrCostFunction = costFunction.getAttributeCostFunction(qFunction);
			double scalingConst = costFunction.getScalingConstant(qFunction);
			String rewardItems = buildRewardItems(transFunction, qFunction, attrCostFunction, scalingConst);
			builder.append(rewardItems);
		}
		builder.append("endrewards");
		return builder.toString();
	}

	/**
	 * Build reward items for a given QA function.
	 * 
	 * @param transFunction
	 * @param qFunction
	 * @param attrCostFunction
	 * @param scalingConst
	 * @return computeCost & {actionTypeName}={encoded action value} & {srcVarName}={value} ... & {destVarName}={value}
	 *         ... : {scaled cost}; ...
	 * @throws VarNotFoundException
	 * @throws AttributeNameNotFoundException
	 * @throws IncompatibleVarException
	 * @throws DiscriminantNotFoundException
	 * @throws ActionNotFoundException
	 * @throws ActionDefinitionNotFoundException
	 */
	String buildRewardItems(TransitionFunction transFunction, IQFunction qFunction,
			AttributeCostFunction<IQFunction> attrCostFunction, double scalingConst)
			throws VarNotFoundException, AttributeNameNotFoundException, IncompatibleVarException,
			DiscriminantNotFoundException, ActionNotFoundException, ActionDefinitionNotFoundException {
		TransitionDefinition transDef = qFunction.getTransitionDefinition();
		Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs = transDef.getSrcStateVarDefs();
		Set<StateVarDefinition<IStateVarValue>> destStateVarDefs = transDef.getDestStateVarDefs();
		ActionDefinition<IAction> actionDef = transDef.getActionDef();
		FactoredPSO<IAction> actionPSO = transFunction.getActionPSO(actionDef);

		StringBuilder builder = new StringBuilder();
		String actionTypeName = actionDef.getName();

		for (IAction action : actionDef.getActions()) {
			Integer encodedActionValue = mEncodings.getEncodedIntValue(actionDef, action);

			Set<Set<StateVar<IStateVarValue>>> srcCombinations = getApplicableSrcValuesCombinations(actionPSO, action,
					srcStateVarDefs);
			for (Set<StateVar<IStateVarValue>> srcVars : srcCombinations) {
				String srcPartialGuard = buildPartialRewardGuard(srcVars, PrismTranslatorUtilities.SRC_SUFFIX);

				Set<Set<StateVar<IStateVarValue>>> destCombinations = getPossibleDestValuesCombination(actionPSO,
						action, destStateVarDefs, srcVars);
				for (Set<StateVar<IStateVarValue>> destVars : destCombinations) {
					String destPartialGuard = buildPartialRewardGuard(destVars);

					Transition trans = new Transition(action, srcVars, destVars);
					double attrCost = attrCostFunction.getCost(trans);
					double scaledAttrCost = scalingConst * attrCost;

					builder.append(PrismTranslatorUtilities.INDENT);
					builder.append("computeCost & ");
					builder.append(actionTypeName);
					builder.append("=");
					builder.append(encodedActionValue);
					builder.append(" & ");
					builder.append(srcPartialGuard);
					builder.append(" & ");
					builder.append(destPartialGuard);
					builder.append(" : ");
					builder.append(scaledAttrCost);
					builder.append(";");
					builder.append("\n");
				}
			}
		}
		return builder.toString();
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

	private Set<Set<StateVar<IStateVarValue>>> getApplicableSrcValuesCombinations(FactoredPSO<IAction> actionPSO,
			IAction action, Set<StateVarDefinition<IStateVarValue>> srcStateVarDefs) throws ActionNotFoundException {
		Precondition precond = actionPSO.getPrecondition(action);
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> srcVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> srcVarDef : srcStateVarDefs) {
			Set<IStateVarValue> applicableVals = precond.getApplicableValues(srcVarDef);
			srcVarValues.put(srcVarDef, applicableVals);
		}
		return getCombinations(srcVarValues);
	}

	private Set<Set<StateVar<IStateVarValue>>> getPossibleDestValuesCombination(FactoredPSO<IAction> actionPSO,
			IAction action, Set<StateVarDefinition<IStateVarValue>> destStateVarDefs,
			Set<StateVar<IStateVarValue>> srcVars) throws IncompatibleVarException, VarNotFoundException,
			DiscriminantNotFoundException, ActionNotFoundException {
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
				StateVar<IStateVarValue> discrVar = new StateVar<>(var.getDefinition(), var.getValue());
				discriminant.add(discrVar);
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
			StateVar<IStateVarValue> newVar = new StateVar<>(varDef, value);
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
	 * 
	 * @param goal
	 * @return R{"cost"}min=? [ F {varName}={encoded int value} & ... ]
	 * @throws VarNotFoundException
	 */
	String buildGoalProperty(State goal) throws VarNotFoundException {
		StringBuilder builder = new StringBuilder();
		builder.append("R{\"");
		builder.append(COST_STRUCTURE_NAME);
		builder.append("\"}min=? [ F ");
		boolean firstVar = true;
		for (StateVar<IStateVarValue> goalVar : goal) {
			Integer encodedValue = mEncodings.getEncodedIntValue(goalVar.getDefinition(), goalVar.getValue());
			if (!firstVar) {
				builder.append(" & ");
			} else {
				firstVar = false;
			}
			builder.append(goalVar.getName());
			builder.append("=");
			builder.append(encodedValue);
		}
		builder.append(" ]");
		return builder.toString();
	}

}
