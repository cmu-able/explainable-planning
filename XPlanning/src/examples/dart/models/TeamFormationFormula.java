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
 * {@link TeamFormationFormula} is the formula of the effect on "teamFormation" of {@link ChangeFormAction} actions.
 * 
 * @author rsukkerd
 *
 */
public class TeamFormationFormula implements IProbabilisticTransitionFormula<ChangeFormAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Discriminant/effect variable
	private StateVarDefinition<TeamFormation> mTeamFormDef;

	private EffectClass mEffectClass; // of teamAltitude

	public TeamFormationFormula(StateVarDefinition<TeamFormation> teamFormDef) {
		mTeamFormDef = teamFormDef;

		mEffectClass = new EffectClass();
		mEffectClass.add(teamFormDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, ChangeFormAction changeForm) throws XMDPException {
		// Target teamFormation variable
		StateVar<TeamFormation> teamFormDest = mTeamFormDef.getStateVar(changeForm.getTargetFormation());

		ProbabilisticEffect teamFormProbEffect = new ProbabilisticEffect(mEffectClass);
		Effect newFormEffect = new Effect(mEffectClass);
		newFormEffect.add(teamFormDest);
		teamFormProbEffect.put(newFormEffect, 1.0);
		return teamFormProbEffect;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TeamFormationFormula)) {
			return false;
		}
		TeamFormationFormula formula = (TeamFormationFormula) obj;
		return formula.mTeamFormDef.equals(mTeamFormDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTeamFormDef.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
