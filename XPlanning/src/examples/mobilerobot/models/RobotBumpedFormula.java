package examples.mobilerobot.models;

import language.domain.models.IProbabilisticTransitionFormula;
import language.domain.models.StateVar;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.Effect;
import language.mdp.EffectClass;
import language.mdp.ProbabilisticEffect;

/**
 * {@link RobotBumpedFormula} is a formula of the probability of the robot bumping when it moves.
 * 
 * @author rsukkerd
 *
 */
public class RobotBumpedFormula implements IProbabilisticTransitionFormula<MoveToAction> {

	private static final double BUMP_PROB_PARTIALLY_OCCLUDED = 0.2;
	private static final double BUMP_PROB_OCCLUDED = 0.4;
	private static final double BUMP_PROB_CLEAR = 0.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocSrcDef;
	private StateVarDefinition<RobotBumped> mrBumpedDestDef;

	private EffectClass mEffectClass; // of rBumped

	public RobotBumpedFormula(StateVarDefinition<Location> rLocSrcDef, StateVarDefinition<RobotBumped> rBumpedDestDef) {
		mrLocSrcDef = rLocSrcDef;
		mrBumpedDestDef = rBumpedDestDef;

		mEffectClass = new EffectClass();
		mEffectClass.add(rBumpedDestDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, MoveToAction moveTo) throws XMDPException {
		Location srcLoc = discriminant.getStateVarValue(Location.class, mrLocSrcDef);
		StateVar<Location> rLocSrc = mrLocSrcDef.getStateVar(srcLoc);
		Occlusion occlusion = moveTo.getOcclusion(rLocSrc);
		ProbabilisticEffect rBumpedProbEffect = new ProbabilisticEffect(mEffectClass);
		// Possible effects on rBumped
		Effect bumpedEffect = new Effect(mEffectClass);
		Effect notBumpedEffect = new Effect(mEffectClass);
		RobotBumped bumped = new RobotBumped(true);
		RobotBumped notBumped = new RobotBumped(false);
		bumpedEffect.add(mrBumpedDestDef.getStateVar(bumped));
		notBumpedEffect.add(mrBumpedDestDef.getStateVar(notBumped));

		if (occlusion == Occlusion.PARTIALLY_OCCLUDED) {
			rBumpedProbEffect.put(bumpedEffect, BUMP_PROB_PARTIALLY_OCCLUDED);
			rBumpedProbEffect.put(notBumpedEffect, 1 - BUMP_PROB_PARTIALLY_OCCLUDED);
		} else if (occlusion == Occlusion.OCCLUDED) {
			rBumpedProbEffect.put(bumpedEffect, BUMP_PROB_OCCLUDED);
			rBumpedProbEffect.put(notBumpedEffect, 1 - BUMP_PROB_OCCLUDED);
		} else if (occlusion == Occlusion.CLEAR) {
			rBumpedProbEffect.put(bumpedEffect, BUMP_PROB_CLEAR);
			rBumpedProbEffect.put(notBumpedEffect, 1 - BUMP_PROB_CLEAR);
		} else {
			throw new IllegalArgumentException("Unknown occlusion value: " + occlusion);
		}

		return rBumpedProbEffect;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RobotBumpedFormula)) {
			return false;
		}
		RobotBumpedFormula formula = (RobotBumpedFormula) obj;
		return formula.mrLocSrcDef.equals(mrLocSrcDef) && formula.mrBumpedDestDef.equals(mrBumpedDestDef);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrLocSrcDef.hashCode();
			result = 31 * result + mrBumpedDestDef.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
