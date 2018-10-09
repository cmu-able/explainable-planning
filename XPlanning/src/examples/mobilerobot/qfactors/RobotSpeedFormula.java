package examples.mobilerobot.qfactors;

import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.Precondition;
import language.mdp.ProbabilisticEffect;

public class RobotSpeedFormula implements IProbabilisticTransitionFormula<SetSpeedAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<RobotSpeed> mrSpeedDef;
	private Precondition<SetSpeedAction> mPrecondition;
	private EffectClass mEffectClass; // of rSpeed

	public RobotSpeedFormula(StateVarDefinition<RobotSpeed> rSpeedDef, Precondition<SetSpeedAction> precondition) {
		mrSpeedDef = rSpeedDef;
		mPrecondition = precondition;

		mEffectClass = new EffectClass();
		mEffectClass.add(rSpeedDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, SetSpeedAction setSpeed) throws XMDPException {
		ProbabilisticEffect rSpeedProbEffect = new ProbabilisticEffect(mEffectClass);
		Effect newSpeedEffect = new Effect(mEffectClass);
		StateVar<RobotSpeed> newSpeed = mrSpeedDef.getStateVar(setSpeed.getTargetSpeed());
		newSpeedEffect.add(newSpeed);
		rSpeedProbEffect.put(newSpeedEffect, 1.0);
		return rSpeedProbEffect;
	}

	@Override
	public Precondition<SetSpeedAction> getPrecondition() {
		return mPrecondition;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RobotSpeedFormula)) {
			return false;
		}
		RobotSpeedFormula formula = (RobotSpeedFormula) obj;
		return formula.mrSpeedDef.equals(mrSpeedDef) && formula.mPrecondition.equals(mPrecondition);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrSpeedDef.hashCode();
			result = 31 * result + mPrecondition.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
