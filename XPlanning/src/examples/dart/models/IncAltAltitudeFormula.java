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
 * {@link IncAltAltitudeFormula} is the formula of the effect on "teamAltitude" of {@link IncAltAction} actions.
 * 
 * @author rsukkerd
 *
 */
public class IncAltAltitudeFormula implements IProbabilisticTransitionFormula<IncAltAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Discriminant/effect variable
	private StateVarDefinition<TeamAltitude> mTeamAltDef;

	private EffectClass mEffectClass; // of teamAltitude

	public IncAltAltitudeFormula(StateVarDefinition<TeamAltitude> teamAltDef) {
		mTeamAltDef = teamAltDef;

		mEffectClass = new EffectClass();
		mEffectClass.add(teamAltDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, IncAltAction incAlt) throws XMDPException {
		TeamAltitude currAlt = discriminant.getStateVarValue(TeamAltitude.class, mTeamAltDef); // current altitude
		TeamAltitude altChange = incAlt.getAltitudeChange(); // change in altitude
		TeamAltitude targetAlt = new TeamAltitude(currAlt.getAltitudeLevel() + altChange.getAltitudeLevel());

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
		if (!(obj instanceof IncAltAltitudeFormula)) {
			return false;
		}
		IncAltAltitudeFormula formula = (IncAltAltitudeFormula) obj;
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
