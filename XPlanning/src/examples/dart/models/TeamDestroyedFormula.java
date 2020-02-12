package examples.dart.models;

import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.ProbabilisticEffect;

/**
 * {@link TeamDestroyedFormula} is the formula of the probability of the team being destroyed during any action of type
 * {@link IDurativeAction}.
 * 
 * @author rsukkerd
 *
 */
public class TeamDestroyedFormula<E extends IDurativeAction> implements IProbabilisticTransitionFormula<E> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	// Discriminant variables
	private StateVarDefinition<TeamAltitude> mAltSrcDef;
	private StateVarDefinition<TeamFormation> mFormSrcDef;
	private StateVarDefinition<TeamECM> mECMSrcDef;
	private StateVarDefinition<RouteSegment> mSegmentSrcDef;

	// Effect variable
	private StateVarDefinition<TeamDestroyed> mDestroyedDef;

	private EffectClass mEffectClass; // of teamDestroyed

	// Constants
	private double mThreatRange; // at an altitude of r_T or higher, threats cannot shoot down the team
	private double mPsi; // factor by which the probability of being destroyed is reduced due to flying in tight
							// formation

	public TeamDestroyedFormula(StateVarDefinition<TeamAltitude> altSrcDef,
			StateVarDefinition<TeamFormation> formSrcDef, StateVarDefinition<TeamECM> ecmSrcDef,
			StateVarDefinition<RouteSegment> segmentSrcDef, StateVarDefinition<TeamDestroyed> destroyedDef,
			double threatRange, double psi) {
		mAltSrcDef = altSrcDef;
		mFormSrcDef = formSrcDef;
		mECMSrcDef = ecmSrcDef;
		mSegmentSrcDef = segmentSrcDef;
		mDestroyedDef = destroyedDef;

		mThreatRange = threatRange;
		mPsi = psi;

		mEffectClass = new EffectClass();
		mEffectClass.add(destroyedDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, E action) throws XMDPException {
		// If team is already destroyed, teamDestroyed stays true
		TeamDestroyed srcDestroyed = discriminant.getStateVarValue(TeamDestroyed.class, mDestroyedDef);

		double destroyedProb;
		if (srcDestroyed.isDestroyed()) {
			destroyedProb = 1;
		} else {
			// Determining factors of effect on teamDestroyed
			TeamAltitude srcAlt = discriminant.getStateVarValue(TeamAltitude.class, mAltSrcDef);
			TeamFormation srcForm = discriminant.getStateVarValue(TeamFormation.class, mFormSrcDef);
			TeamECM srcECM = discriminant.getStateVarValue(TeamECM.class, mECMSrcDef);
			RouteSegment srcSegment = discriminant.getStateVarValue(RouteSegment.class, mSegmentSrcDef);

			// Use the average altitude of the team during the segment to compute probability of being destroyed
			double avgAltitude = srcAlt.getAltitudeLevel();

			// If the action changes teamAltitude (i.e., incAlt and decAlt actions), compute the average altitude
			if (action.getNamePrefix().equals("incAlt") || action.getNamePrefix().equals("decAlt")) {
				// Get altitude change parameter
				TeamAltitude altChange = (TeamAltitude) action.getParameters().get(0);

				int sign = action.getNamePrefix().equals("incAlt") ? 1 : -1;
				avgAltitude += sign * altChange.getAltitudeLevel() / 2.0;
			}

			double altTerm = Math.max(0, mThreatRange - avgAltitude) / mThreatRange;
			int phi = srcForm.getFormation().equals("loose") ? 0 : 1; // loose: phi = 0, tight: phi = 1
			double formTerm = (1 - phi) + phi / mPsi;
			int ecm = srcECM.isECMOn() ? 1 : 0;
			double ecmTerm = (1 - ecm) + ecm / 4.0;

			// Probability of being destroyed, given threat exists in the segment
			double destroyedProbGivenThreat = altTerm * formTerm * ecmTerm;

			// Probability of being destroyed
			destroyedProb = srcSegment.getThreatDistribution().getExpectedThreatProbability()
					* destroyedProbGivenThreat;
		}

		// Possible effects on teamDestroyed
		Effect destroyedEffect = new Effect(mEffectClass);
		Effect notDestroyedEffect = new Effect(mEffectClass);
		TeamDestroyed destroyed = new TeamDestroyed(true);
		TeamDestroyed notDestroyed = new TeamDestroyed(false);
		destroyedEffect.add(mDestroyedDef.getStateVar(destroyed));
		notDestroyedEffect.add(mDestroyedDef.getStateVar(notDestroyed));

		// Probabilistic Effect on teamDestroyed
		ProbabilisticEffect destroyedProbEffect = new ProbabilisticEffect(mEffectClass);
		destroyedProbEffect.put(destroyedEffect, destroyedProb);
		destroyedProbEffect.put(notDestroyedEffect, 1 - destroyedProb);

		return destroyedProbEffect;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TeamDestroyedFormula<?>)) {
			return false;
		}
		TeamDestroyedFormula<?> formula = (TeamDestroyedFormula<?>) obj;
		return formula.mAltSrcDef.equals(mAltSrcDef) && formula.mFormSrcDef.equals(mFormSrcDef)
				&& formula.mECMSrcDef.equals(mECMSrcDef) && formula.mSegmentSrcDef.equals(mSegmentSrcDef)
				&& formula.mDestroyedDef.equals(mDestroyedDef)
				&& Double.compare(formula.mThreatRange, mThreatRange) == 0 && Double.compare(formula.mPsi, mPsi) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAltSrcDef.hashCode();
			result = 31 * result + mFormSrcDef.hashCode();
			result = 31 * result + mECMSrcDef.hashCode();
			result = 31 * result + mSegmentSrcDef.hashCode();
			result = 31 * result + mDestroyedDef.hashCode();
			result = 31 * result + Double.hashCode(mThreatRange);
			result = 31 * result + Double.hashCode(mPsi);
			hashCode = result;
		}
		return hashCode;
	}

}
