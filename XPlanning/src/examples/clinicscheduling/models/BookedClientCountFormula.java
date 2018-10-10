package examples.clinicscheduling.models;

import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.ProbabilisticEffect;

public class BookedClientCountFormula implements IProbabilisticTransitionFormula<ScheduleAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<ClientCount> mBookedClientCountDef;
	private StateVarDefinition<ABP> mABPDef;
	private StateVarDefinition<ClientCount> mNewClientCountDef;
	private EffectClass mEffectClass; // of bookedClientCount

	public BookedClientCountFormula(StateVarDefinition<ClientCount> bookedClientCountDef,
			StateVarDefinition<ABP> abpDef, StateVarDefinition<ClientCount> newClientCountDef) {
		mBookedClientCountDef = bookedClientCountDef;
		mABPDef = abpDef;
		mNewClientCountDef = newClientCountDef;

		mEffectClass = new EffectClass();
		mEffectClass.add(bookedClientCountDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, ScheduleAction schedule) throws XMDPException {
		ClientCount currBookedClientCount = discriminant.getStateVarValue(ClientCount.class, mBookedClientCountDef);
		ClientCount newClientCount = discriminant.getStateVarValue(ClientCount.class, mNewClientCountDef);
		ABP currABP = discriminant.getStateVarValue(ABP.class, mABPDef);
		ClientCount servicedNewClientCount = schedule.getNumNewClientsToService();

		int x = currBookedClientCount.getValue();
		int y = newClientCount.getValue();
		int w = currABP.getValue();
		int b = servicedNewClientCount.getValue();

		int numBookedClients = x - Math.min(x, w) + y - b;
		ClientCount newBookedClientCount = new ClientCount(numBookedClients);

		ProbabilisticEffect bookedClientCountProbEffect = new ProbabilisticEffect(mEffectClass);
		Effect newBookedClientCountEffect = new Effect(mEffectClass);
		StateVar<ClientCount> destBookedClientCount = mBookedClientCountDef.getStateVar(newBookedClientCount);
		newBookedClientCountEffect.add(destBookedClientCount);
		bookedClientCountProbEffect.put(newBookedClientCountEffect, 1.0);
		return bookedClientCountProbEffect;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof BookedClientCountFormula)) {
			return false;
		}
		BookedClientCountFormula formula = (BookedClientCountFormula) obj;
		return formula.mBookedClientCountDef.equals(mBookedClientCountDef) && formula.mABPDef.equals(mABPDef)
				&& formula.mNewClientCountDef.equals(mNewClientCountDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mBookedClientCountDef.hashCode();
			result = 31 * result + mABPDef.hashCode();
			result = 31 * result + mNewClientCountDef.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
