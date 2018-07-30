package examples.mobilerobot.factors;

import factors.IProbabilisticTransitionFormula;
import factors.StateVar;
import factors.StateVarDefinition;
import language.exceptions.IncompatibleVarException;
import language.exceptions.XMDPException;
import mdp.Discriminant;
import mdp.Effect;
import mdp.EffectClass;
import mdp.Precondition;
import mdp.ProbabilisticEffect;

/**
 * {@link RobotBumpedFormula} is a formula of the probability of the robot bumping when it moves.
 * 
 * @author rsukkerd
 *
 */
public class RobotBumpedFormula implements IProbabilisticTransitionFormula<MoveToAction> {

	private static final double BUMP_PROB_PARTIALLY_OCCLUDED = 0.2;
	private static final double BUMP_PROB_BLOCKED = 1.0;
	private static final double BUMP_PROB_CLEAR = 0.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocSrcDef;
	private StateVarDefinition<RobotBumped> mrBumpedDestDef;
	private Precondition<MoveToAction> mPrecondition;

	private EffectClass mEffectClass; // of rBumped
	private Effect mBumpedEffect;
	private Effect mNotBumpedEffect;

	public RobotBumpedFormula(StateVarDefinition<Location> rLocSrcDef, StateVarDefinition<RobotBumped> rBumpedDestDef,
			Precondition<MoveToAction> precondition) throws IncompatibleVarException {
		mrLocSrcDef = rLocSrcDef;
		mrBumpedDestDef = rBumpedDestDef;
		mPrecondition = precondition;

		mEffectClass = new EffectClass();
		mEffectClass.add(rBumpedDestDef);
		// Possible effects on rBumped
		mBumpedEffect = new Effect(mEffectClass);
		mNotBumpedEffect = new Effect(mEffectClass);
		RobotBumped bumped = new RobotBumped(true);
		RobotBumped notBumped = new RobotBumped(false);
		mBumpedEffect.add(mrBumpedDestDef.getStateVar(bumped));
		mNotBumpedEffect.add(mrBumpedDestDef.getStateVar(notBumped));
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, MoveToAction moveTo) throws XMDPException {
		Location srcLoc = discriminant.getStateVarValue(Location.class, mrLocSrcDef);
		StateVar<Location> rLocSrc = mrLocSrcDef.getStateVar(srcLoc);
		Occlusion occlusion = moveTo.getOcclusion(rLocSrc);
		ProbabilisticEffect rBumpedProbEffect = new ProbabilisticEffect(mEffectClass);

		if (occlusion == Occlusion.PARTIALLY_OCCLUDED) {
			rBumpedProbEffect.put(mBumpedEffect, BUMP_PROB_PARTIALLY_OCCLUDED);
			rBumpedProbEffect.put(mNotBumpedEffect, 1 - BUMP_PROB_PARTIALLY_OCCLUDED);
		} else if (occlusion == Occlusion.BLOCKED) {
			rBumpedProbEffect.put(mBumpedEffect, BUMP_PROB_BLOCKED);
		} else {
			rBumpedProbEffect.put(mNotBumpedEffect, 1 - BUMP_PROB_CLEAR);
		}

		return rBumpedProbEffect;
	}

	@Override
	public Precondition<MoveToAction> getPrecondition() {
		return mPrecondition;
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
		return formula.mrLocSrcDef.equals(mrLocSrcDef) && formula.mrBumpedDestDef.equals(mrBumpedDestDef)
				&& formula.mPrecondition.equals(mPrecondition);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrLocSrcDef.hashCode();
			result = 31 * result + mrBumpedDestDef.hashCode();
			result = 31 * result + mPrecondition.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
