package hmodel;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import explanation.analysis.PolicyInfo;
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
import language.objectives.CostCriterion;
import prism.PrismException;
import solver.prismconnector.PrismConnector;
import solver.prismconnector.PrismConnectorSettings;
import solver.prismconnector.exceptions.ResultParsingException;

public class HModelGenerator {

	private PolicyInfo mQueryPolicyInfo;
	private XMDP mQueryXMDP;
	private CostCriterion mCostCriterion;
	private PrismConnectorSettings mPrismConnSettings;

	// Inputs to be set from the user's why-not query
	private StateVarTuple mQueryState;
	private ProbabilisticEffect mNewIniStateDist;

	public HModelGenerator(PolicyInfo queryPolicyInfo, CostCriterion costCriterion,
			PrismConnectorSettings prismConnSettings) {
		mQueryPolicyInfo = queryPolicyInfo;
		mQueryXMDP = queryPolicyInfo.getXMDP();
		mCostCriterion = costCriterion;
		mPrismConnSettings = prismConnSettings;
	}

	public void query(StateVarTuple queryState, IAction queryAction) throws XMDPException {
		mQueryState = queryState;

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
		mNewIniStateDist = combinedProbEffect;
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

	public double computeQAValueConstraint(IQFunction<?, ?> queryQFunction)
			throws PrismException, ResultParsingException, XMDPException {
		// Create XMDP model identical to the original model, but with the query state as initial state
		XMDP xmdp = new XMDP(mQueryXMDP.getStateSpace(), mQueryXMDP.getActionSpace(), mQueryState, mQueryXMDP.getGoal(),
				mQueryXMDP.getTransitionFunction(), mQueryXMDP.getQSpace(), mQueryXMDP.getCostFunction());

		// Create Prism connector (without the query state as absorbing state)
		PrismConnector prismConnector = new PrismConnector(xmdp, mCostCriterion, mPrismConnSettings);

		// Compute QA value of the query policy, starting from the query state
		double queryQAValue = prismConnector.computeQAValue(mQueryPolicyInfo.getPolicy(), queryQFunction);

		// Close down PRISM
		prismConnector.terminate();

		return queryQAValue;
	}

	public Set<PrismConnector> buildPrismConnectorsForHModels() throws PrismException {
		Set<PrismConnector> prismConnectors = new HashSet<>();

		for (Entry<Effect, Double> e : mNewIniStateDist) {
			Effect effect = e.getKey();
			StateVarTuple newIniState = new StateVarTuple();
			newIniState.addStateVarTuple(effect);

			// Create HModel identical to the original XMDP model, but with the resulting state of the why-not query as initial state
			XMDP xmdpHModel = new XMDP(mQueryXMDP.getStateSpace(), mQueryXMDP.getActionSpace(), newIniState,
					mQueryXMDP.getGoal(), mQueryXMDP.getTransitionFunction(), mQueryXMDP.getQSpace(),
					mQueryXMDP.getCostFunction());

			// Create Prism connector with the query state as absorbing state
			PrismConnector prismConnHModel = new PrismConnector(xmdpHModel, mQueryState, mCostCriterion,
					mPrismConnSettings);
			prismConnectors.add(prismConnHModel);
		}

		return prismConnectors;
	}
}
