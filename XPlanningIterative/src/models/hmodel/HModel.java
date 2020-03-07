package models.hmodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import language.domain.metrics.IQFunction;
import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVar;
import language.exceptions.IncompatibleEffectClassException;
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

	// Why-not (s,a) query
	private StateVarTuple mQueryState;
	private E mQueryAction;

	// -- Derived fields -- //

	// Resulting states and their probabilities
	private ProbabilisticEffect mProbEffectOfQuery;

	// XMDPs with the resulting states of the query as initial states
	private Map<StateVarTuple, XMDP> mQueryXMDPs = new HashMap<>();

	// -- End derived fields -- //

	// QA value constraints for alternative policy, starting from each resulting state of the query onwards
	private Map<StateVarTuple, Map<IQFunction<?, ?>, Double>> mQueryQAValueConstraints = new HashMap<>();

	public HModel(XMDP originalXMDP, StateVarTuple queryState, E queryAction) throws XMDPException {
		mOriginalXMDP = originalXMDP;
		mQueryState = queryState;
		mQueryAction = queryAction;
		mProbEffectOfQuery = computeProbabilisticEffect(queryState, queryAction);
		createQueryXMDPs();
	}

	private ProbabilisticEffect computeProbabilisticEffect(StateVarTuple queryState, E queryAction)
			throws XMDPException {
		ActionDefinition<E> actionDef = mOriginalXMDP.getActionSpace().getActionDefinition(queryAction);

		// Use <? super E> because if the query action is a constituent action that doesn't have its own
		// actionPSO, we need to use its parent composite actionPSO
		FactoredPSO<? super E> actionPSO = null;

		// If the query action is a constituent action, we must use its parent composite actionPSO
		// to get the full effect of the query action
		FactoredPSO<? super E> parentCompositeActionPSO = null;

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
			addProbabilisticEffects(directEffectClasses, actionPSO, queryState, queryAction, probEffects);
		}

		if (parentCompositeActionPSO != null) {
			// The query action has parent composite actionPSO
			// Add the parent probabilistic effects of the query action
			Set<EffectClass> parentEffectClasses = parentCompositeActionPSO.getIndependentEffectClasses();
			addProbabilisticEffects(parentEffectClasses, parentCompositeActionPSO, queryState, queryAction,
					probEffects);
		}

		// Combined probabilistic effect of the (s,q) query
		return combineProbabilisticEffects(probEffects);
	}

	private void addProbabilisticEffects(Set<EffectClass> effectClasses, FactoredPSO<? super E> actionPSO,
			StateVarTuple queryState, E queryAction, Set<ProbabilisticEffect> probEffectsOutput) throws XMDPException {
		for (EffectClass effectClass : effectClasses) {
			IActionDescription<? super E> actionDesc = actionPSO.getActionDescription(effectClass);
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

			// New initial state
			StateVarTuple newIniState = new StateVarTuple();

			// Add effect of the query (s_query, a_query)
			newIniState.addStateVarTuple(effect);

			// Set values of the variables unaffected by the query, in the new initial state
			for (StateVar<IStateVarValue> stateVar : mQueryState) {
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

	public XMDP getOriginalXMDP() {
		return mOriginalXMDP;
	}

	public StateVarTuple getQueryState() {
		return mQueryState;
	}

	public E getQueryAction() {
		return mQueryAction;
	}

	public ProbabilisticEffect getProbabilisticEffectOfQuery() {
		return mProbEffectOfQuery;
	}

	public Set<StateVarTuple> getAllDestStatesOfQuery() {
		return mQueryXMDPs.keySet();
	}

	public XMDP getQueryXMDP(StateVarTuple newIniState) {
		return mQueryXMDPs.get(newIniState);
	}

	public void putQueryQAValueConstraint(StateVarTuple newIniState, IQFunction<?, ?> qFunction,
			double queryQAValueConstraint) {
		if (!mQueryQAValueConstraints.containsKey(newIniState)) {
			mQueryQAValueConstraints.put(newIniState, new HashMap<>());
		}

		mQueryQAValueConstraints.get(newIniState).put(qFunction, queryQAValueConstraint);
	}

	public double getQueryQAValueConstraint(StateVarTuple newIniState, IQFunction<?, ?> qFunction) {
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
				&& hModel.mQueryAction.equals(mQueryAction)
				&& hModel.mQueryQAValueConstraints.equals(mQueryQAValueConstraints);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mOriginalXMDP.hashCode();
			result = 31 * result + mQueryState.hashCode();
			result = 31 * result + mQueryAction.hashCode();
			result = 31 * result + mQueryQAValueConstraints.hashCode();
			hashCode = result;
		}
		return result;
	}

}
