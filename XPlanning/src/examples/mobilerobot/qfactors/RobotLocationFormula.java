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

/**
 * {@link RobotLocationFormula} is a formula of the probability of the robot's next location when it moves.
 * 
 * @author rsukkerd
 *
 */
public class RobotLocationFormula implements IProbabilisticTransitionFormula<MoveToAction> {

	private static final double MOVE_PROB_NONBLOCKED = 1.0;
	private static final double MOVE_PROB_BLOCKED = 0.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocDef;
	private Precondition<MoveToAction> mPrecondition;
	private EffectClass mEffectClass; // of rLoc

	public RobotLocationFormula(StateVarDefinition<Location> rLocDef, Precondition<MoveToAction> precondition) {
		mrLocDef = rLocDef;
		mPrecondition = precondition;

		mEffectClass = new EffectClass();
		mEffectClass.add(rLocDef);
	}

	@Override
	public ProbabilisticEffect formula(Discriminant discriminant, MoveToAction moveTo) throws XMDPException {
		Location srcLoc = discriminant.getStateVarValue(Location.class, mrLocDef);
		StateVar<Location> rLocSrc = mrLocDef.getStateVar(srcLoc);
		Occlusion occlusion = moveTo.getOcclusion(rLocSrc);
		ProbabilisticEffect rLocProbEffect = new ProbabilisticEffect(mEffectClass);
		// Possible effects on rLoc
		Effect newLoc = new Effect(mEffectClass);
		Effect oldLoc = new Effect(mEffectClass);
		newLoc.add(mrLocDef.getStateVar(moveTo.getDestination()));
		oldLoc.add(rLocSrc);

		if (occlusion == Occlusion.BLOCKED) {
			rLocProbEffect.put(oldLoc, 1 - MOVE_PROB_BLOCKED);
		} else {
			rLocProbEffect.put(newLoc, MOVE_PROB_NONBLOCKED);
		}

		return rLocProbEffect;
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
		if (!(obj instanceof RobotLocationFormula)) {
			return false;
		}
		RobotLocationFormula formula = (RobotLocationFormula) obj;
		return formula.mrLocDef.equals(mrLocDef) && formula.mPrecondition.equals(mPrecondition);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrLocDef.hashCode();
			result = 31 * result + mPrecondition.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
