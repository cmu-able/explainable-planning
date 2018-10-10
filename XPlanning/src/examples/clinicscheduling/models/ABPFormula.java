package examples.clinicscheduling.models;

import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.Precondition;
import language.mdp.ProbabilisticEffect;

public class ABPFormula implements IProbabilisticTransitionFormula<ScheduleAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<ABP> mABPDef;
	private Precondition<ScheduleAction> mPrecondition;
	private EffectClass mEffectClass; // of ABP

	public ABPFormula(StateVarDefinition<ABP> abpDef, Precondition<ScheduleAction> precondition) {
		mABPDef = abpDef;
		mPrecondition = precondition;

		mEffectClass = new EffectClass();
		mEffectClass.add(abpDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, ScheduleAction schedule) throws XMDPException {
		ProbabilisticEffect abpProbEffect = new ProbabilisticEffect(mEffectClass);
		Effect newABPEffect = new Effect(mEffectClass);
		StateVar<ABP> abpDest = mABPDef.getStateVar(schedule.getNewABP());
		newABPEffect.add(abpDest);
		abpProbEffect.put(newABPEffect, 1.0);
		return abpProbEffect;
	}

	@Override
	public Precondition<ScheduleAction> getPrecondition() {
		return mPrecondition;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ABPFormula)) {
			return false;
		}
		ABPFormula formula = (ABPFormula) obj;
		return formula.mABPDef.equals(mABPDef) && formula.mPrecondition.equals(mPrecondition);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mABPDef.hashCode();
			result = 31 * result + mPrecondition.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
