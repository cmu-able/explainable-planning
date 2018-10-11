package examples.clinicscheduling.models;

import org.apache.commons.math3.util.ArithmeticUtils;

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
	private EffectClass mEffectClass; // of newClientCount

	public NewClientCountFormula(StateVarDefinition<ClientCount> newClientCountDef, double clientArrivalRate) {
		mNewClientCountDef = newClientCountDef;
		mClientArrivalRate = clientArrivalRate;

		mEffectClass = new EffectClass();
		mEffectClass.add(newClientCountDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, ScheduleAction action) throws XMDPException {
		ProbabilisticEffect newClientCountProbEffect = new ProbabilisticEffect(mEffectClass);

		// Possible effects on newClientCount
		for (ClientCount newClientCount : mNewClientCountDef.getPossibleValues()) {
			Effect numClientsEffect = new Effect(mEffectClass);
			numClientsEffect.add(mNewClientCountDef.getStateVar(newClientCount));

			// A possible number of new clients arriving today
			int numNewClients = newClientCount.getValue();

			// Probability of the above number of new clients arriving today
			// Poisson distribution: P(k events in a day) = e^(-lambda) * lambda^k / k!
			double probNumClients = Math.pow(Math.E, -1 * mClientArrivalRate)
					* Math.pow(mClientArrivalRate, numNewClients) / ArithmeticUtils.factorial(numNewClients);

			newClientCountProbEffect.put(numClientsEffect, probNumClients);
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
				&& Double.compare(formula.mClientArrivalRate, mClientArrivalRate) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNewClientCountDef.hashCode();
			result = 31 * result + Double.hashCode(mClientArrivalRate);
			hashCode = result;
		}
		return hashCode;
	}

}
