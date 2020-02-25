package solver.prismconnector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import language.domain.metrics.IEvent;
import language.domain.metrics.IQFunction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.Transition;
import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.ActionNotFoundException;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.StateVarClassNotFoundException;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.EffectClass;
import language.mdp.FactoredPSO;
import language.mdp.IActionDescription;
import language.mdp.Precondition;
import language.mdp.ProbabilisticEffect;
import language.mdp.StateVarClass;
import language.mdp.StateVarTuple;
import language.mdp.TransitionFunction;
import language.objectives.AttributeCostFunction;
import language.objectives.IAdditiveCostFunction;
import solver.prismconnector.PrismTranslatorHelper.ActionFilter;

public class PrismRewardTranslatorHelper {

	private static final String BEGIN_REWARDS = "rewards \"%s\"";
	private static final String END_REWARDS = "endrewards";

	private TransitionFunction mTransFunction;
	private ValueEncodingScheme mEncodings;
	private ActionFilter mActionFilter;

	public PrismRewardTranslatorHelper(TransitionFunction transFunction, ValueEncodingScheme encodings,
			ActionFilter actionFilter) {
		mTransFunction = transFunction;
		mEncodings = encodings;
		mActionFilter = actionFilter;
	}

	/**
	 * Build a transition-reward structure for a given objective function. This can be the cost function of MDP or an
	 * arbitrary objective function.
	 * 
	 * The objective function must be the first reward structure in any PRISM MDP translation.
	 * 
	 * @param objectiveFunction
	 *            : Objective function that this reward structure represents
	 * @return rewards "{objective name}" ... endrewards
	 * @throws XMDPException
	 */
	String buildRewardStructure(IAdditiveCostFunction objectiveFunction) throws XMDPException {
		String sanitizedRewardName = PrismTranslatorUtils.sanitizeNameString(objectiveFunction.getName());
		StringBuilder builder = new StringBuilder();
		builder.append(String.format(BEGIN_REWARDS, sanitizedRewardName));
		builder.append("\n");

		Set<IQFunction<IAction, ITransitionStructure<IAction>>> qFunctions = objectiveFunction.getQFunctions();

		for (IQFunction<IAction, ITransitionStructure<IAction>> qFunction : qFunctions) {
			AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> attrCostFunction = objectiveFunction
					.getAttributeCostFunction(qFunction);
			double scalingConst = objectiveFunction.getScalingConstant(attrCostFunction);

			ITransitionStructure<IAction> domain = qFunction.getTransitionStructure();
			FactoredPSO<IAction> actionPSO = mTransFunction.getActionPSO(domain.getActionDef());
			TransitionEvaluator<IAction, ITransitionStructure<IAction>> evaluator = new TransitionEvaluator<IAction, ITransitionStructure<IAction>>() {

				@Override
				public double evaluate(Transition<IAction, ITransitionStructure<IAction>> transition)
						throws VarNotFoundException, AttributeNameNotFoundException {
					double qValue = qFunction.getValue(transition);
					double attrCost = attrCostFunction.getCost(qValue);
					return scalingConst * attrCost;
				}
			};

			String rewardItems = buildRewardItems(domain, actionPSO, evaluator);
			builder.append(rewardItems);
		}

		// Add auxiliary reward if necessary (e.g., for SSPs)
		double offset = objectiveFunction.getOffset();
		String artificialReward = buildAuxiliaryRewardItem(offset);
		builder.append(artificialReward);

		builder.append("\n");
		builder.append(END_REWARDS);
		return builder.toString();
	}

	/**
	 * Build a list of transition-reward structures for a given set of QA functions.
	 * 
	 * The order of the reward structures conforms to the {@link QFunctionEncodingScheme}.
	 * 
	 * @param qFunctions
	 *            : QA functions
	 * @return Reward structures for the QA functions
	 * @throws XMDPException
	 */
	String buildRewardStructures(Iterable<IQFunction<IAction, ITransitionStructure<IAction>>> qFunctions)
			throws XMDPException {
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
			String rewards = buildRewardStructure(qFunction);
			builder.append(rewards);
		}
		return builder.toString();
	}

	/**
	 * Build a transition-reward structure for a given QA function.
	 * 
	 * @param qFunction
	 *            : QA function
	 * @return rewards "{QA name}" ... endrewards
	 * @throws XMDPException
	 */
	<E extends IAction, T extends ITransitionStructure<E>> String buildRewardStructure(IQFunction<E, T> qFunction)
			throws XMDPException {
		String rewardName = qFunction.getName();
		T domain = qFunction.getTransitionStructure();
		FactoredPSO<E> actionPSO = mTransFunction.getActionPSO(domain.getActionDef());
		TransitionEvaluator<E, T> evaluator = new TransitionEvaluator<E, T>() {

			@Override
			public double evaluate(Transition<E, T> transition)
					throws VarNotFoundException, AttributeNameNotFoundException {
				return qFunction.getValue(transition);
			}
		};
		return buildRewardStructure(rewardName, domain, actionPSO, evaluator);
	}

	/**
	 * Build a list of transition-reward structures for a given set of single-attribute cost functions of QAs.
	 * 
	 * @param attrCostFunctions
	 *            : Single-attribute cost functions of QAs
	 * @return Reward structures for the QA cost functions
	 * @throws XMDPException
	 */
	String buildRewardStructuresForQACostFunctions(
			Iterable<AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>>> attrCostFunctions)
			throws XMDPException {
		StringBuilder builder = new StringBuilder();
		builder.append("// QA Cost Functions\n\n");
		boolean first = true;
		for (AttributeCostFunction<IQFunction<IAction, ITransitionStructure<IAction>>> attrCostFunction : attrCostFunctions) {
			if (!first) {
				builder.append("\n\n");
			} else {
				first = false;
			}
			builder.append("// ");
			builder.append(attrCostFunction.getName());
			builder.append("\n\n");
			String rewards = buildRewardStructure(attrCostFunction);
			builder.append(rewards);
		}
		return builder.toString();
	}

	/**
	 * Build a transition-reward structure for a given QA cost function.
	 * 
	 * @param attrCostFunction
	 *            : Single-attribute cost function of QA
	 * @return rewards "cost_{QA name}" ... endrewards
	 * @throws XMDPException
	 */
	<E extends IAction, T extends ITransitionStructure<E>, S extends IQFunction<E, T>> String buildRewardStructure(
			AttributeCostFunction<S> attrCostFunction) throws XMDPException {
		String rewardName = attrCostFunction.getName();
		T domain = attrCostFunction.getQFunction().getTransitionStructure();
		FactoredPSO<E> actionPSO = mTransFunction.getActionPSO(domain.getActionDef());
		TransitionEvaluator<E, T> evaluator = new TransitionEvaluator<E, T>() {

			@Override
			public double evaluate(Transition<E, T> transition)
					throws VarNotFoundException, AttributeNameNotFoundException {
				S qFunction = attrCostFunction.getQFunction();
				return attrCostFunction.getCost(qFunction.getValue(transition));
			}
		};
		return buildRewardStructure(rewardName, domain, actionPSO, evaluator);
	}

	/**
	 * Build a list of reward structures for counting events.
	 * 
	 * @param events
	 *            : Events to be counted
	 * @return Reward structures for counting the events.
	 * @throws XMDPException
	 */
	String buildRewardStructuresForEventCounts(Set<? extends IEvent<?, ?>> events) throws XMDPException {
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
			String rewards = buildRewardStructureForEventCount(event);
			builder.append(rewards);
		}
		return builder.toString();
	}

	/**
	 * Build a reward structure for counting a particular event.
	 * 
	 * @param event
	 *            : Event to be counted
	 * @return Reward structure for counting the event.
	 * @throws XMDPException
	 */
	<E extends IAction, T extends ITransitionStructure<E>> String buildRewardStructureForEventCount(IEvent<E, T> event)
			throws XMDPException {
		String rewardName = event.getName() + "_count";
		T eventStructure = event.getTransitionStructure();
		FactoredPSO<E> actionPSO = mTransFunction.getActionPSO(eventStructure.getActionDef());
		TransitionEvaluator<E, T> evaluator = new TransitionEvaluator<E, T>() {

			@Override
			public double evaluate(Transition<E, T> transition)
					throws VarNotFoundException, AttributeNameNotFoundException {
				return event.getEventProbability(transition);
			}
		};
		return buildRewardStructure(rewardName, eventStructure, actionPSO, evaluator);
	}

	private <E extends IAction, T extends ITransitionStructure<E>> String buildRewardStructure(String rewardName,
			T domain, FactoredPSO<E> actionPSO, TransitionEvaluator<E, T> evaluator) throws XMDPException {
		String sanitizedRewardName = PrismTranslatorUtils.sanitizeNameString(rewardName);
		StringBuilder builder = new StringBuilder();
		builder.append(String.format(BEGIN_REWARDS, sanitizedRewardName));
		builder.append("\n");
		String rewardItems = buildRewardItems(domain, actionPSO, evaluator);
		builder.append(rewardItems);
		builder.append(END_REWARDS);
		return builder.toString();
	}

	/**
	 * Build transition-reward items for a given evaluator. The reward values may represent either: (1)~a scaled cost of
	 * the QA of each transition, (2)~an actual value of the QA of each transition, or (3)~occurrence of a particular
	 * event, depending on the given evaluator.
	 * 
	 * @param transStructure
	 *            : Transition structure -- this may be the domain of a QA function
	 * @param actionPSO
	 *            : PSO of the corresponding action type
	 * @param evaluator
	 *            : A function that assigns a value to a transition
	 * @return [{actionName}] {srcVarName}={value} & ... {discrVarName}={value} & ... : {expected value}; ...
	 * @throws XMDPException
	 */
	<E extends IAction, T extends ITransitionStructure<E>> String buildRewardItems(T transStructure,
			FactoredPSO<E> actionPSO, TransitionEvaluator<E, T> evaluator) throws XMDPException {
		StateVarClass srcStateVarClass = transStructure.getSrcStateVarClass();
		StateVarClass destStateVarClass = transStructure.getDestStateVarClass();
		ActionDefinition<E> actionDef = transStructure.getActionDef();

		StringBuilder builder = new StringBuilder();

		for (E action : actionDef.getActions()) {
			if (!mActionFilter.filterAction(action)) {
				// Skip actions that are not present in the model (in the case of DTMC)
				continue;
			}

			Set<StateVarTuple> srcCombinations = getApplicableSrcValuesCombinations(srcStateVarClass, action,
					actionPSO);

			for (StateVarTuple srcVars : srcCombinations) {
				if (destStateVarClass.isEmpty()) {
					// Transition structure has no destination variable
					// Reward value r(s,a) can be computed from srcVars and action
					StateVarTuple emptyVarTuple = new StateVarTuple();
					Transition<E, T> transition = new Transition<>(transStructure, action, srcVars, emptyVarTuple);
					double transValue = evaluator.evaluate(transition);
					String rewardItem = buildRewardItem(srcVars, emptyVarTuple, action, transValue);
					builder.append(PrismTranslatorUtils.INDENT);
					builder.append(rewardItem);
					builder.append("\n");
				} else {
					// Transition structure has destination variables
					// Reward value r(s,a) must be computed from the expectation of r'(s,a,s') over all s'
					Set<StateVarTuple> discrCombinations = getApplicableDiscriminantCombinations(destStateVarClass,
							srcVars, actionPSO, action);

					for (StateVarTuple applicableDiscrVars : discrCombinations) {
						double expectedValue = computeExpectedTransitionValue(transStructure, actionPSO, evaluator,
								srcVars, applicableDiscrVars, action);
						String rewardItem = buildRewardItem(srcVars, applicableDiscrVars, action, expectedValue);
						builder.append(PrismTranslatorUtils.INDENT);
						builder.append(rewardItem);
						builder.append("\n");
					}
				}
			}
		}
		return builder.toString();
	}

	/**
	 * Build a transition-reward item for a QA function of the form Q_i(s,a,s'). The reward value may represent either a
	 * QA value or a QA cost of a transition.
	 * 
	 * @param srcVars
	 *            : Source variables of the QA function
	 * @param applicableDiscrVars
	 *            : Discriminant variables of the destination variables of the QA function (that are applicable given
	 *            the source variables and the action)
	 * @param action
	 *            : Action of the QA function
	 * @param expectedValue
	 *            : Reward value, which is the expected value of C/Q_i(s,a) = sum_s'(Pr(s'|s,a) * C/Q_i(s,a,s'))
	 * @return [{actionName}] {srcVarName}={value} & ... {discrVarName}={value} & ... : {expected value};
	 * @throws VarNotFoundException
	 */
	String buildRewardItem(StateVarTuple srcVars, StateVarTuple applicableDiscrVars, IAction action,
			double expectedValue) throws VarNotFoundException {
		StateVarTuple combinedVars = new StateVarTuple();
		combinedVars.addStateVarTuple(srcVars);
		combinedVars.addStateVarTuple(applicableDiscrVars);
		String sanitizedActionName = PrismTranslatorUtils.sanitizeNameString(action.getName());
		String combinedVarsExpr = PrismTranslatorUtils.buildExpression(combinedVars, mEncodings);

		StringBuilder builder = new StringBuilder();
		builder.append("[");
		builder.append(sanitizedActionName);
		builder.append("] ");
		if (!combinedVarsExpr.isEmpty()) {
			builder.append(combinedVarsExpr);
		} else {
			builder.append("true");
		}
		builder.append(" : ");
		builder.append(expectedValue);
		builder.append(";");
		return builder.toString();
	}

	/**
	 * Build a transition-reward item for a QA function of the form Q_i(s). The reward value may represent either a QA
	 * value or a QA cost of a state.
	 * 
	 * @param srcVars
	 *            : Source variables of the QA function
	 * @param value
	 *            : Reward value of C/Q_i(s)
	 * @return [compute] {srcVarName}={value} & ... : {value};
	 * @throws VarNotFoundException
	 */
	String buildRewardItem(StateVarTuple srcVars, double value) throws VarNotFoundException {
		String srcVarsExpr = PrismTranslatorUtils.buildExpression(srcVars, mEncodings);

		StringBuilder builder = new StringBuilder();
		builder.append("[compute] ");
		if (!srcVarsExpr.isEmpty()) {
			builder.append(srcVarsExpr);
		} else {
			builder.append("true");
		}
		builder.append(" : ");
		builder.append(value);
		builder.append(";");
		return builder.toString();
	}

	/**
	 * NOTE: Auxiliary reward is only used for SSPs to ensure that all objective costs are positive, except in the goal
	 * states. In the case of average-cost MDPs, auxiliary reward is not used (i.e., "offset" is 0), and PRISM is not
	 * used to solve such problems (GRB solver is used instead).
	 * 
	 * This is to ensure that there is no zero-reward cycle in the MDP. This is because the current version of PRISM 4.4
	 * does not support "constructing a strategy for Rmin in the presence of zero-reward ECs".
	 * 
	 * @param value
	 *            : Auxiliary reward value assigned to every "compute" transition
	 * @return [compute] true : {value};
	 */
	String buildAuxiliaryRewardItem(double value) {
		return PrismTranslatorUtils.INDENT + "[compute] true : " + value + ";";
	}

	/**
	 * Generate all applicable value combinations of a given set of source variables. The applicable combinations are
	 * determined by action precondition.
	 * 
	 * If there is no source variable definition, then this method returns a singleton set of an empty
	 * {@link StateVarTuple}.
	 * 
	 * @param srcStateVarClasss
	 *            : Source variable definitions
	 * @param action
	 *            : Action
	 * @param actionPSO
	 * @return All applicable source value combinations
	 * @throws ActionNotFoundException
	 * @throws StateVarClassNotFoundException
	 */
	private <E extends IAction> Set<StateVarTuple> getApplicableSrcValuesCombinations(StateVarClass srcStateVarClasss,
			E action, FactoredPSO<E> actionPSO) throws ActionNotFoundException, StateVarClassNotFoundException {
		Precondition<E> precondition = actionPSO.getPrecondition();

		if (precondition.hasMultivarPredicateOn(srcStateVarClasss)) {
			// Precondition has a multivariate predicate exactly on the variables in srcStateVarClass
			// Get all applicable source value tuples from the precondition
			return precondition.getApplicableTuples(action, srcStateVarClasss);
		} else if (precondition.hasMultivarPredicatePartiallyOn(srcStateVarClasss)) {
			// Precondition has a multivariate predicate that is partially on the variables in srcStateVarClass
			// Get all partially applicable source value tuples from the precondition
			return precondition.getPartialApplicableTuples(action, srcStateVarClasss);
		}

		// Assume that there is no multivariate predicate in the precondition that is on SOME of the variables in
		// srcStateVarClass.
		// Therefore, each variable in srcStateVarClass can at most have 1 univariate predicate on it.

		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> srcVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> srcVarDef : srcStateVarClasss) {
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
	 * @param destStateVarClass
	 *            : Destination variable definitions
	 * @param srcVars
	 *            : Source variables
	 * @param action
	 *            : Action
	 * @param actionPSO
	 * @return All possible destination value combinations
	 * @throws XMDPException
	 */
	private <E extends IAction> Set<StateVarTuple> getPossibleDestValuesCombinations(StateVarClass destStateVarClass,
			StateVarTuple srcVars, E action, FactoredPSO<E> actionPSO) throws XMDPException {
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> destVarValues = new HashMap<>();
		for (StateVarDefinition<IStateVarValue> destVarDef : destStateVarClass) {
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
		Precondition<E> precond = actionPSO.getPrecondition();

		// If precondition has a multivariate predicate on the discriminant class, get all applicable discriminants
		// from precondition
		if (precond.hasMultivarPredicateOn(discrClass.getStateVarClass())) {
			return getApplicableDiscriminantsFromPrecondition(precond, discrClass, action, srcVars);
		}

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

	private <E extends IAction> Set<Discriminant> getApplicableDiscriminantsFromPrecondition(Precondition<E> precond,
			DiscriminantClass discrClass, E action, StateVarTuple srcVars)
			throws ActionNotFoundException, StateVarClassNotFoundException, VarNotFoundException {
		Set<StateVarTuple> applicableTuples = precond.getApplicableTuples(action, discrClass.getStateVarClass(),
				srcVars);
		Set<Discriminant> applicableDiscriminants = new HashSet<>();
		for (StateVarTuple applicableTuple : applicableTuples) {
			Discriminant applicableDiscr = new Discriminant(discrClass);
			applicableDiscr.addAllRelevant(applicableTuple);
			applicableDiscriminants.add(applicableDiscr);
		}
		return applicableDiscriminants;
	}

	/**
	 * Get all combinations of discriminants of a set of destination variables, given source values and action.
	 * 
	 * @param destStateVarClass
	 * @param srcVars
	 * @param actionPSO
	 * @param action
	 * @return All combinations of applicable discriminants of all destination variables, given source values and action
	 * @throws XMDPException
	 */
	private <E extends IAction> Set<StateVarTuple> getApplicableDiscriminantCombinations(
			StateVarClass destStateVarClass, StateVarTuple srcVars, FactoredPSO<E> actionPSO, E action)
			throws XMDPException {
		// All applicable discriminants of all destination variables, given srcVars and action
		Map<StateVarDefinition<IStateVarValue>, Set<IStateVarValue>> allApplicableDiscriminants = new HashMap<>();

		for (StateVarDefinition<IStateVarValue> destVarDef : destStateVarClass) {
			Set<Discriminant> applicableDiscriminants = getApplicableDiscriminants(destVarDef, srcVars, actionPSO,
					action);

			for (Discriminant applicableDiscriminant : applicableDiscriminants) {
				for (StateVar<IStateVarValue> discriminantVar : applicableDiscriminant) {
					StateVarDefinition<IStateVarValue> discriminantVarDef = discriminantVar.getDefinition();
					IStateVarValue discriminantValue = discriminantVar.getValue();

					if (!allApplicableDiscriminants.containsKey(discriminantVarDef)) {
						allApplicableDiscriminants.put(discriminantVarDef, new HashSet<>());
					}

					allApplicableDiscriminants.get(discriminantVarDef).add(discriminantValue);
				}
			}
		}

		return getCombinations(allApplicableDiscriminants);
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
	 * Partition destination variables into groups, where each group is affected by a given action type independently of
	 * other groups.
	 * 
	 * @param destVars
	 * @param actionPSO
	 * @return Independently-affected groups of destination variables, given an action type
	 * @throws VarNotFoundException
	 */
	private Map<EffectClass, StateVarTuple> partitionIndependentDestVars(StateVarTuple destVars,
			FactoredPSO<? extends IAction> actionPSO) throws VarNotFoundException {
		Map<EffectClass, StateVarTuple> partitionedDestVars = new HashMap<>();
		Set<EffectClass> effectClasses = actionPSO.getIndependentEffectClasses();
		for (EffectClass effectClass : effectClasses) {
			StateVarTuple group = new StateVarTuple();
			for (StateVarDefinition<IStateVarValue> varDef : effectClass) {
				if (destVars.contains(varDef)) {
					IStateVarValue value = destVars.getStateVarValue(IStateVarValue.class, varDef);
					group.addStateVar(varDef.getStateVar(value));
				}
			}
			partitionedDestVars.put(effectClass, group);
		}
		return partitionedDestVars;
	}

	/**
	 * Compute the expected value of taking an action in a source state.
	 * 
	 * @param transStructure
	 * @param actionPSO
	 * @param evaluator
	 *            : Transition evaluator
	 * @param srcVars
	 *            : Source variables
	 * @param applicableDiscrVars
	 *            : Applicable discriminant variables of all destination variables of transStructure
	 * @param action
	 *            : Action
	 * @return Expected value of taking an action in a source state
	 * @throws XMDPException
	 */
	private <E extends IAction, T extends ITransitionStructure<E>> double computeExpectedTransitionValue(
			T transStructure, FactoredPSO<E> actionPSO, TransitionEvaluator<E, T> evaluator, StateVarTuple srcVars,
			StateVarTuple applicableDiscrVars, E action) throws XMDPException {
		double expectedTransValue = 0;

		Set<StateVarTuple> destCombinations = getPossibleDestValuesCombinations(transStructure.getDestStateVarClass(),
				srcVars, action, actionPSO);

		// Assume: all destination variables of any TransitionStructure are affected by its action
		for (StateVarTuple destVars : destCombinations) {
			Map<EffectClass, StateVarTuple> indepDestVarGroups = partitionIndependentDestVars(destVars, actionPSO);
			double destVarsProb = 1;

			for (Entry<EffectClass, StateVarTuple> e : indepDestVarGroups.entrySet()) {
				EffectClass effectClass = e.getKey();
				StateVarTuple indepDestVarGroup = e.getValue();

				// Filter discriminants of the effect class from applicableDiscrVars
				IActionDescription<E> actionDesc = actionPSO.getActionDescription(effectClass);
				DiscriminantClass discriminantClass = actionDesc.getDiscriminantClass();
				Discriminant discriminant = new Discriminant(discriminantClass);
				for (StateVarDefinition<IStateVarValue> varDef : discriminantClass) {
					IStateVarValue value = applicableDiscrVars.getStateVarValue(IStateVarValue.class, varDef);
					StateVar<IStateVarValue> discrVar = varDef.getStateVar(value);
					discriminant.add(discrVar);
				}
				ProbabilisticEffect probEffect = actionDesc.getProbabilisticEffect(discriminant, action);
				double indepDestVarGroupProb = probEffect.getMarginalProbability(indepDestVarGroup);
				destVarsProb *= indepDestVarGroupProb;
			}

			Transition<E, T> transition = new Transition<>(transStructure, action, srcVars, destVars);
			double transValue = evaluator.evaluate(transition);
			expectedTransValue += destVarsProb * transValue;
		}

		return expectedTransValue;
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
