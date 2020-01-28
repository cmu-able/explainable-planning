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
 * {@link DecAltAltitudeFormula} is the formula of the effect on "teamAltitude" of {@link DecAltAction} actions.
 * 
 * @author rsukkerd
 *
 */
public class DecAltAltitudeFormula implements IProbabilisticTransitionFormula<DecAltAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Discriminant/effect variable
	private StateVarDefinition<TeamAltitude> mTeamAltDef;

	private EffectClass mEffectClass; // of teamAltitude

	public DecAltAltitudeFormula(StateVarDefinition<TeamAltitude> teamAltDef) {
		mTeamAltDef = teamAltDef;

		mEffectClass = new EffectClass();
		mEffectClass.add(teamAltDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, DecAltAction decAlt) throws XMDPException {
		TeamAltitude currAlt = discriminant.getStateVarValue(TeamAltitude.class, mTeamAltDef); // current altitude
		TeamAltitude altChange = decAlt.getAltitudeChange(); // change in altitude
		TeamAltitude targetAlt = new TeamAltitude(currAlt.getAltitudeLevel() - altChange.getAltitudeLevel());

		// Target teamAltitude variable
		StateVar<TeamAltitude> teamAltDest = mTeamAltDef.getStateVar(targetAlt);

		ProbabilisticEffect teamAltProbEffect = new ProbabilisticEffect(mEffectClass);
		Effect newAltEffect = new Effect(mEffectClass);
		newAltEffect.add(teamAltDest);
		teamAltProbEffect.put(newAltEffect, 1.0);
		return teamAltProbEffect;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DecAltAltitudeFormula)) {
			return false;
		}
		DecAltAltitudeFormula formula = (DecAltAltitudeFormula) obj;
		return formula.mTeamAltDef.equals(mTeamAltDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mTeamAltDef.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
