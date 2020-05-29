package models.hmodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.StreamSupport;

import language.domain.metrics.IQFunction;
import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.IncompatibleEffectClassException;
import language.exceptions.VarNotFoundException;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.FactoredPSO;
import language.mdp.IActionDescription;
import language.mdp.ProbabilisticEffect;
import language.mdp.StateVarTuple;
import language.mdp.XMDP;

public class HModel<E extends IAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Original XMDP
	private XMDP mOriginalXMDP;

	// -- Why-not (s,a) query -- //
	// Optionally with pre-configuration actions

	// First query state (may not be the final query state)
	private StateVarTuple mQueryState;

	// Non-repeatable query state variable
	// HPolicy must not revisit this state value
	private StateVar<? extends IStateVarValue> mNonRepeatableQueryStateVar;

	// Main query action (final query action)
	private E mQueryAction;

	// Configuration actions to apply prior to the main query action
	private List<? extends IAction> mPreConfigActions;

	// -- End why-not query -- //

	// -- Derived fields -- //

	// Mapping from each state to its pre-configuration action, starting from the query state
	private Map<StateVarTuple, IAction> mPreConfigPolicyConstraints;

	// Final query state after applying all pre-configuration actions
	// The main query action will be applied to this final query state
	private StateVarTuple mFinalQueryState;

	// Resulting states and their probabilities
	private ProbabilisticEffect mProbEffectOfQuery;

	// XMDPs with the resulting states of the query as initial states
	private Map<StateVarTuple, XMDP> mQueryXMDPs = new HashMap<>();

	// -- End derived fields -- //

	// QA value constraints for alternative policy, starting from each resulting state of the query onwards
	private Map<StateVarTuple, Map<IQFunction<?, ?>, Double>> mQueryQAValueConstraints = new HashMap<>();

	public HModel(XMDP originalXMDP, StateVarTuple queryState, E queryAction, List<? extends IAction> preConfigActions,
			String nonRepeatableQueryVarName) throws XMDPException {
		mOriginalXMDP = originalXMDP;
		mQueryState = queryState;
		mQueryAction = queryAction;
		mPreConfigActions = preConfigActions;

		if (nonRepeatableQueryVarName != null) {
			// Set non-repeatable query state variable
			setNonRepeatableQueryStateVar(nonRepeatableQueryVarName);
		}

		// Derived fields
		mPreConfigPolicyConstraints = buildPreConfigPolicyConstraints();
		mProbEffectOfQuery = computeProbabilisticEffect(mFinalQueryState, queryAction);
		createQueryXMDPs();
	}

	/**
	 * Build pre-configuration policy constraints, in a form of a mapping from each state to its pre-configuration
	 * action, starting from the query state.
	 * 
	 * This mapping does NOT include the final query state and the final (main) query action.
	 * 
	 * @return Mapping from each state to its pre-configuration action, starting from the query state
	 * @throws XMDPException
	 */
	private Map<StateVarTuple, IAction> buildPreConfigPolicyConstraints() throws XMDPException {
		Map<StateVarTuple, IAction> preConfigPolicyConstraints = new HashMap<>();

		// currState must contain all variables, because it will be placed in a Policy
		StateVarTuple currState = mQueryState;

		for (IAction preConfigAction : mPreConfigActions) {
			// Map state -> pre-configuration action
			preConfigPolicyConstraints.put(currState, preConfigAction);

			ProbabilisticEffect probEffect = computeProbabilisticEffect(currState, preConfigAction);

			// Assume that any pre-configuration action has deterministic effect

			// detEffect may not contain all variables
			Effect detEffect = StreamSupport.stream(probEffect.spliterator(), false).filter(e -> e.getValue() == 1)
					.map(Map.Entry::getKey).findFirst().orElse(null);

			// nextState must contain all variables
			StateVarTuple nextState = new StateVarTuple();
			nextState.addStateVarTuple(currState); // initialize vars of nextState with values of currState
			nextState.addStateVarTuple(detEffect); // update (some) vars of nextState with values of detEffect

			currState = nextState;
		}

		// Final state after applying all pre-configuration actions
		// Final query state is NOT included in the pre-configuration policy constraints
		mFinalQueryState = currState;

		return preConfigPolicyConstraints;
	}

	private <F extends IAction> ProbabilisticEffect computeProbabilisticEffect(StateVarTuple state, F action)
			throws XMDPException {
		ActionDefinition<F> actionDef = mOriginalXMDP.getActionSpace().getActionDefinition(action);

		// Use <? super F> because if the query action is a constituent action that doesn't have its own
		// actionPSO, we need to use its parent composite actionPSO
		FactoredPSO<? super F> actionPSO = null;

		// If the query action is a constituent action, we must use its parent composite actionPSO
		// to get the full effect of the query action
		FactoredPSO<? super F> parentCompositeActionPSO = null;

		// Check if the query action has its own actionPSO
		if (mOriginalXMDP.getTransitionFunction().hasActionPSO(actionDef)) {
			// The query action has its own actionPSO
			actionPSO = mOriginalXMDP.getTransitionFunction().getActionPSO(actionDef);
		}

		// The query action is a constituent action that doesn't have its own actionPSO
		// (e.g., Fly and Tick actions in DART domain)
		// Use its parent composite actionPSO

		// Check if the query action has parent composite actionPSO
		ActionDefinition<IAction> parentCompositeActionDef = actionDef.getParentCompositeActionDefinition();
		if (parentCompositeActionDef != null) {
			// The query action has parent composite actionPSO
			parentCompositeActionPSO = mOriginalXMDP.getTransitionFunction().getActionPSO(parentCompositeActionDef);
		}

		// All probabilistic effects of the query action, including both its direct effects, 
		// and its parent effects
		Set<ProbabilisticEffect> probEffects = new HashSet<>();

		if (actionPSO != null) {
			// The query action has its own actionPSO
			// Add the direct probabilistic effects of the query action
			Set<EffectClass> directEffectClasses = actionPSO.getIndependentEffectClasses();
			addProbabilisticEffects(directEffectClasses, actionPSO, state, action, probEffects);
		}

		if (parentCompositeActionPSO != null) {
			// The query action has parent composite actionPSO
			// Add the parent probabilistic effects of the query action
			Set<EffectClass> parentEffectClasses = parentCompositeActionPSO.getIndependentEffectClasses();
			addProbabilisticEffects(parentEffectClasses, parentCompositeActionPSO, state, action, probEffects);
		}

		// Combined probabilistic effect of the (s,q) query
		return combineProbabilisticEffects(probEffects);
	}

	private <F extends IAction> void addProbabilisticEffects(Set<EffectClass> effectClasses,
			FactoredPSO<? super F> actionPSO, StateVarTuple queryState, F queryAction,
			Set<ProbabilisticEffect> probEffectsOutput) throws XMDPException {
		for (EffectClass effectClass : effectClasses) {
			IActionDescription<? super F> actionDesc = actionPSO.getActionDescription(effectClass);
			DiscriminantClass discrClass = actionDesc.getDiscriminantClass();

			Discriminant discriminant = new Discriminant(discrClass);
			discriminant.addAllRelevant(queryState);
			ProbabilisticEffect probEffect = actionDesc.getProbabilisticEffect(discriminant, queryAction);

			probEffectsOutput.add(probEffect);
		}
	}

	private ProbabilisticEffect combineProbabilisticEffects(Set<ProbabilisticEffect> probEffects)
			throws IncompatibleEffectClassException {
		Iterator<ProbabilisticEffect> iter = probEffects.iterator();
		ProbabilisticEffect probEffect = iter.next();
		iter.remove();
		return combineProbabilisticEffectsHelper(probEffect, probEffects);
	}

	private ProbabilisticEffect combineProbabilisticEffectsHelper(ProbabilisticEffect probEffect,
			Set<ProbabilisticEffect> otherProbEffects) throws IncompatibleEffectClassException {
		if (otherProbEffects.isEmpty()) {
			return probEffect;
		}

		Iterator<ProbabilisticEffect> iter = otherProbEffects.iterator();
		ProbabilisticEffect otherProbEffect = iter.next();
		iter.remove();

		EffectClass combinedEffectClass = new EffectClass();
		combinedEffectClass.addAll(probEffect.getEffectClass());
		combinedEffectClass.addAll(otherProbEffect.getEffectClass());

		ProbabilisticEffect combinedProbEffect = new ProbabilisticEffect(combinedEffectClass);

		for (Entry<Effect, Double> eA : probEffect) {
			Effect effectA = eA.getKey();
			double probA = eA.getValue();

			for (Entry<Effect, Double> eB : otherProbEffect) {
				Effect effectB = eB.getKey();
				double probB = eB.getValue();

				Effect combinedEffect = new Effect(combinedEffectClass);
				combinedEffect.addAll(effectA);
				combinedEffect.addAll(effectB);

				double combinedProb = probA * probB;

				combinedProbEffect.put(combinedEffect, combinedProb);
			}
		}

		return combineProbabilisticEffectsHelper(combinedProbEffect, otherProbEffects);
	}

	private void createQueryXMDPs() {
		for (Entry<Effect, Double> e : mProbEffectOfQuery) {
			Effect effect = e.getKey();
			double prob = e.getValue();

			// Skip effect that has 0 probability
			// For simplicity, assume for now that the query cannot lead to more than 1 non-absorbing state
			if (prob == 0) {
				continue;
			}

			// New initial state
			StateVarTuple newIniState = new StateVarTuple();

			// Add effect of the query (s_query_final, a_query_final)
			newIniState.addStateVarTuple(effect);

			// Set values of the variables unaffected by the query, in the new initial state
			for (StateVar<IStateVarValue> stateVar : mFinalQueryState) {
				if (!effect.contains(stateVar.getDefinition())) {
					newIniState.addStateVar(stateVar);
				}
			}

			// Create HModel identical to the original XMDP model, but with the resulting state of the why-not query as initial state
			XMDP queryXMDP = new XMDP(mOriginalXMDP.getStateSpace(), mOriginalXMDP.getActionSpace(), newIniState,
					mOriginalXMDP.getGoal(), mOriginalXMDP.getTransitionFunction(), mOriginalXMDP.getQSpace(),
					mOriginalXMDP.getCostFunction());

			mQueryXMDPs.put(newIniState, queryXMDP);
		}

	}

	private void setNonRepeatableQueryStateVar(String stateVarName) throws VarNotFoundException {
		StateVarDefinition<IStateVarValue> varDef = mOriginalXMDP.getStateSpace().getStateVarDefinition(stateVarName);
		IStateVarValue value = mQueryState.getStateVarValue(IStateVarValue.class, varDef);
		mNonRepeatableQueryStateVar = varDef.getStateVar(value);
	}

	public XMDP getOriginalXMDP() {
		return mOriginalXMDP;
	}

	public boolean hasNonRepeatableQueryPredicate() {
		return mNonRepeatableQueryStateVar != null;
	}

	public StateVarTuple getNonRepeatableQueryPredicate() {
		StateVarTuple queryPred = new StateVarTuple();
		queryPred.addStateVar(mNonRepeatableQueryStateVar);
		return queryPred;
	}

	/**
	 * Get pre-configuration policy constraints, in a form of a mapping from each state to its pre-configuration action,
	 * starting from the query state.
	 * 
	 * This mapping does NOT include the final query state and the final (main) query action.
	 * 
	 * @return Mapping from each state to its pre-configuration action, starting from the query state
	 * @throws XMDPException
	 */
	public Map<StateVarTuple, IAction> getPreConfigPolicyConstraints() {
		return mPreConfigPolicyConstraints;
	}

	/**
	 * Get the final query state, which is the state after applying all pre-configuration actions.
	 * 
	 * @return Final query state
	 */
	public StateVarTuple getFinalQueryState() {
		return mFinalQueryState;
	}

	/**
	 * Get the final query action, which is the main query action after pre-configuration actions, if any. This main
	 * query action can have probabilistic effects.
	 * 
	 * @return Final query action
	 */
	public E getFinalQueryAction() {
		return mQueryAction;
	}

	/**
	 * Get probabilistic effect of the full query, including all pre-configuration actions and the main query action.
	 * 
	 * @return Probabilistic effect of the full query
	 */
	public ProbabilisticEffect getProbabilisticEffectOfQuery() {
		return mProbEffectOfQuery;
	}

	/**
	 * For simplicity, assume for now that the query cannot lead to more than 1 non-absorbing state.
	 * 
	 * @return Destination states of the full query
	 */
	public Set<StateVarTuple> getAllDestStatesOfQuery() {
		return mQueryXMDPs.keySet();
	}

	public XMDP getQueryXMDP(StateVarTuple newIniState) {
		return mQueryXMDPs.get(newIniState);
	}

	public void putQAValueConstraint(StateVarTuple newIniState, IQFunction<?, ?> qFunction,
			double queryQAValueConstraint) {
		if (!mQueryQAValueConstraints.containsKey(newIniState)) {
			mQueryQAValueConstraints.put(newIniState, new HashMap<>());
		}

		mQueryQAValueConstraints.get(newIniState).put(qFunction, queryQAValueConstraint);
	}

	public double getQAValueConstraint(StateVarTuple newIniState, IQFunction<?, ?> qFunction) {
		return mQueryQAValueConstraints.get(newIniState).get(qFunction);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HModel<?>)) {
			return false;
		}
		HModel<?> hModel = (HModel<?>) obj;
		return hModel.mOriginalXMDP.equals(mOriginalXMDP) && hModel.mQueryState.equals(mQueryState)
				&& hModel.mNonRepeatableQueryStateVar.equals(mNonRepeatableQueryStateVar)
				&& hModel.mQueryAction.equals(mQueryAction) && hModel.mPreConfigActions.equals(mPreConfigActions)
				&& hModel.mQueryQAValueConstraints.equals(mQueryQAValueConstraints);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mOriginalXMDP.hashCode();
			result = 31 * result + mQueryState.hashCode();
			result = 31 * result + mNonRepeatableQueryStateVar.hashCode();
			result = 31 * result + mQueryAction.hashCode();
			result = 31 * result + mPreConfigActions.hashCode();
			result = 31 * result + mQueryQAValueConstraints.hashCode();
			hashCode = result;
		}
		return result;
	}

}
