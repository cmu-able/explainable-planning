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

	public static final double THREAT_RANGE = 2;
	public static final double PSI = 4;

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
	private StateVarDefinition<TeamDestroyed> mDestroyedDestDef;

	private EffectClass mEffectClass; // of teamDestroyed

	public TeamDestroyedFormula(StateVarDefinition<TeamAltitude> altSrcDef,
			StateVarDefinition<TeamFormation> formSrcDef, StateVarDefinition<TeamECM> ecmSrcDef,
			StateVarDefinition<RouteSegment> segmentSrcDef, StateVarDefinition<TeamDestroyed> destroyedDestDef) {
		mAltSrcDef = altSrcDef;
		mFormSrcDef = formSrcDef;
		mECMSrcDef = ecmSrcDef;
		mSegmentSrcDef = segmentSrcDef;
		mDestroyedDestDef = destroyedDestDef;

		mEffectClass = new EffectClass();
		mEffectClass.add(destroyedDestDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, E action) throws XMDPException {
		// Determining factors of effect on teamDestroyed
		TeamAltitude srcAlt = discriminant.getStateVarValue(TeamAltitude.class, mAltSrcDef);
		TeamFormation srcForm = discriminant.getStateVarValue(TeamFormation.class, mFormSrcDef);
		TeamECM srcECM = discriminant.getStateVarValue(TeamECM.class, mECMSrcDef);
		RouteSegment srcSegment = discriminant.getStateVarValue(RouteSegment.class, mSegmentSrcDef);

		// Possible effects on teamDestroyed
		Effect destroyedEffect = new Effect(mEffectClass);
		Effect notDestroyedEffect = new Effect(mEffectClass);
		TeamDestroyed destroyed = new TeamDestroyed(true);
		TeamDestroyed notDestroyed = new TeamDestroyed(false);
		destroyedEffect.add(mDestroyedDestDef.getStateVar(destroyed));
		notDestroyedEffect.add(mDestroyedDestDef.getStateVar(notDestroyed));

		double altTerm = Math.max(0, THREAT_RANGE - srcAlt.getAltitudeLevel()) / THREAT_RANGE;
		int phi = srcForm.getFormation().equals("loose") ? 0 : 1; // loose: phi = 0, tight: phi = 1
		double formTerm = (1 - phi) + phi / PSI;
		int ecm = srcECM.isECMOn() ? 1 : 0;
		double ecmTerm = (1 - ecm) + ecm / 4.0;

		// Probability of being destroyed, given threat exists in the segment
		double destroyedProbGivenThreat = altTerm * formTerm * ecmTerm;

		// Probability of being destroyed
		double destroyedProb = srcSegment.getThreatDistribution().getExpectedThreatProbability()
				* destroyedProbGivenThreat;

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
				&& formula.mDestroyedDestDef.equals(mDestroyedDestDef);
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
			result = 31 * result + mDestroyedDestDef.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
