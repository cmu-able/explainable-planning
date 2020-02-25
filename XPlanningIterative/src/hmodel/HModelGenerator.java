package hmodel;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import explanation.analysis.PolicyInfo;
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

public class HModelGenerator {

	private PolicyInfo mQueryPolicyInfo;
	private XMDP mQueryXMDP;

	public HModelGenerator(PolicyInfo queryPolicyInfo) {
		mQueryPolicyInfo = queryPolicyInfo;
		mQueryXMDP = queryPolicyInfo.getXMDP();
	}

	public void query(StateVarTuple queryState, IAction queryAction) throws XMDPException {
		ActionDefinition<IAction> actionDef = mQueryXMDP.getActionSpace().getActionDefinition(queryAction);
		FactoredPSO<IAction> actionPSO = mQueryXMDP.getTransitionFunction().getActionPSO(actionDef);
		Set<EffectClass> effectClasses = actionPSO.getIndependentEffectClasses();

		Set<ProbabilisticEffect> probEffects = new HashSet<>();

		for (EffectClass effectClass : effectClasses) {
			IActionDescription<IAction> actionDesc = actionPSO.getActionDescription(effectClass);
			DiscriminantClass discrClass = actionDesc.getDiscriminantClass();

			Discriminant discriminant = new Discriminant(discrClass);
			discriminant.addAllRelevant(queryState);
			ProbabilisticEffect probEffect = actionDesc.getProbabilisticEffect(discriminant, queryAction);

			probEffects.add(probEffect);
		}

		// New initial state(s)
		ProbabilisticEffect combinedProbEffect = combineProbabilisticEffects(probEffects);
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
}
