package examples.clinicscheduling.models;

import java.util.Map;
import java.util.Map.Entry;

import examples.clinicscheduling.metrics.ClientPredictionUtils;
import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.ProbabilisticEffect;

public class NewClientCountFormula implements IProbabilisticTransitionFormula<ScheduleAction> {
	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<ClientCount> mNewClientCountDef;
	private double mClientArrivalRate;
	private int mBranchFactor;
	private EffectClass mEffectClass; // of newClientCount

	public NewClientCountFormula(StateVarDefinition<ClientCount> newClientCountDef, double clientArrivalRate,
			int branchFactor) {
		mNewClientCountDef = newClientCountDef;
		mClientArrivalRate = clientArrivalRate;
		mBranchFactor = branchFactor;

		mEffectClass = new EffectClass();
		mEffectClass.add(newClientCountDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, ScheduleAction action) throws XMDPException {
		ProbabilisticEffect newClientCountProbEffect = new ProbabilisticEffect(mEffectClass);

		Map<ClientCount, Double> newClientCountDistribution = ClientPredictionUtils
				.getNewClientCountDistribution(mClientArrivalRate, mBranchFactor);

		// Possible effects on newClientCount
		for (Entry<ClientCount, Double> e : newClientCountDistribution.entrySet()) {
			ClientCount newClientCount = e.getKey();
			Double probNewClientCount = e.getValue();

			Effect numClientsEffect = new Effect(mEffectClass);
			numClientsEffect.add(mNewClientCountDef.getStateVar(newClientCount));

			newClientCountProbEffect.put(numClientsEffect, probNewClientCount);
		}

		return newClientCountProbEffect;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NewClientCountFormula)) {
			return false;
		}
		NewClientCountFormula formula = (NewClientCountFormula) obj;
		return formula.mNewClientCountDef.equals(mNewClientCountDef)
				&& Double.compare(formula.mClientArrivalRate, mClientArrivalRate) == 0
				&& formula.mBranchFactor == mBranchFactor;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNewClientCountDef.hashCode();
			result = 31 * result + Double.hashCode(mClientArrivalRate);
			result = 31 * result + Integer.hashCode(mBranchFactor);
			hashCode = result;
		}
		return hashCode;
	}

}
