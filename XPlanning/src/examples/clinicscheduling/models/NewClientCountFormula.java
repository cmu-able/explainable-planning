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
	private double mEventRate;
	private int mBranchFactor;
	private EffectClass mEffectClass; // of newClientCount

	public NewClientCountFormula(StateVarDefinition<ClientCount> newClientCountDef, double eventRate,
			int branchFactor) {
		mNewClientCountDef = newClientCountDef;
		mEventRate = eventRate;
		mBranchFactor = branchFactor;

		mEffectClass = new EffectClass();
		mEffectClass.add(newClientCountDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, ScheduleAction action) throws XMDPException {
		// Branching factor is always an odd number
		int numIntervals = (mBranchFactor - 1) / 2 + 1;

		ProbabilisticEffect newClientCountProbEffect = new ProbabilisticEffect(mEffectClass);

		// Possible effects on newClientCount
		for (int i = 1; i <= mBranchFactor; i++) {
			double c = (double) i / numIntervals;
			int numEvents = (int) Math.floor(c * mEventRate);

			// numEvents is a possible number of new clients arriving today
			Effect numClientsEffect = new Effect(mEffectClass);
			ClientCount newClientCount = new ClientCount(numEvents);
			numClientsEffect.add(mNewClientCountDef.getStateVar(newClientCount));

			// Probability of the above number of new clients arriving today
			// Poisson distribution: P(k events in a day) = e^(-lambda) * lambda^k / k!
			double probNumClients = Math.pow(Math.E, -1 * mEventRate) * Math.pow(mEventRate, numEvents)
					/ ArithmeticUtils.factorial(numEvents);

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
				&& Double.compare(formula.mEventRate, mEventRate) == 0 && formula.mBranchFactor == mBranchFactor;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNewClientCountDef.hashCode();
			result = 31 * result + Double.hashCode(mEventRate);
			result = 31 * result + Integer.hashCode(mBranchFactor);
			hashCode = result;
		}
		return hashCode;
	}

}
