package models.hmodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import language.domain.metrics.IQFunction;
import language.domain.models.ActionDefinition;
import language.domain.models.IAction;
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
		FactoredPSO<E> actionPSO = mOriginalXMDP.getTransitionFunction().getActionPSO(actionDef);
		Set<EffectClass> effectClasses = actionPSO.getIndependentEffectClasses();

		Set<ProbabilisticEffect> probEffects = new HashSet<>();

		for (EffectClass effectClass : effectClasses) {
			IActionDescription<E> actionDesc = actionPSO.getActionDescription(effectClass);
			DiscriminantClass discrClass = actionDesc.getDiscriminantClass();

			Discriminant discriminant = new Discriminant(discrClass);
			discriminant.addAllRelevant(queryState);
			ProbabilisticEffect probEffect = actionDesc.getProbabilisticEffect(discriminant, queryAction);

			probEffects.add(probEffect);
		}

		// Combined probabilistic effect of the (s,q) query
		return combineProbabilisticEffects(probEffects);
	}

	private ProbabilisticEffect combineProbabilisticEffects(Set<ProbabilisticEffect> probEffects)
			throws IncompatibleEffectClassException {
		ProbabilisticEffect probEffect = probEffects.iterator().next();
		probEffects.iterator().remove();
		return combineProbabilisticEffectsHelper(probEffect, probEffects);
	}

	private ProbabilisticEffect combineProbabilisticEffectsHelper(ProbabilisticEffect probEffect,
			Set<ProbabilisticEffect> otherProbEffects) throws IncompatibleEffectClassException {
		if (otherProbEffects.isEmpty()) {
			return probEffect;
		}

		ProbabilisticEffect otherProbEffect = otherProbEffects.iterator().next();
		otherProbEffects.iterator().remove();

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
			StateVarTuple newIniState = new StateVarTuple();
			newIniState.addStateVarTuple(effect);

			// Create HModel identical to the original XMDP model, but with the resulting state of the why-not query as initial state
			XMDP queryXMDP = new XMDP(mOriginalXMDP.getStateSpace(), mOriginalXMDP.getActionSpace(), newIniState,
					mOriginalXMDP.getGoal(), mOriginalXMDP.getTransitionFunction(), mOriginalXMDP.getQSpace(),
					mOriginalXMDP.getCostFunction());

			mQueryXMDPs.put(newIniState, queryXMDP);
		}

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
