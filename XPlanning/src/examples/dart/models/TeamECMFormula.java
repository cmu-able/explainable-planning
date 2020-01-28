package examples.dart.models;

import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.ProbabilisticEffect;

/**
 * {@link TeamECMFormula} is the formula of the effect on "teamECM" of {@link SwitchECMAction} actions.
 * 
 * @author rsukkerd
 *
 */
public class TeamECMFormula implements IProbabilisticTransitionFormula<SwitchECMAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Discriminant/effect variable
	private StateVarDefinition<TeamECM> mTeamECMDef;

	private EffectClass mEffectClass; // of teamAltitude

	public TeamECMFormula(StateVarDefinition<TeamECM> teamECMDef) {
		mTeamECMDef = teamECMDef;

		mEffectClass = new EffectClass();
		mEffectClass.add(teamECMDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, SwitchECMAction switchECM) throws XMDPException {
		// Target teamECM variable
		StateVar<TeamECM> teamECMDest = mTeamECMDef.getStateVar(switchECM.getTargetECM());

		ProbabilisticEffect teamECMProbEffect = new ProbabilisticEffect(mEffectClass);
		Effect newECMEffect = new Effect(mEffectClass);
		newECMEffect.add(teamECMDest);
		teamECMProbEffect.put(newECMEffect, 1.0);
		return teamECMProbEffect;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TeamECMFormula)) {
			return false;
		}
		TeamECMFormula formula = (TeamECMFormula) obj;
		return formula.mTeamECMDef.equals(mTeamECMDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTeamECMDef.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
