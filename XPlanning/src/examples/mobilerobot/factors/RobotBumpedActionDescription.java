package examples.mobilerobot.factors;

import java.util.Set;

import exceptions.ActionNotFoundException;
import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;
import exceptions.IncompatibleActionException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import factors.ActionDefinition;
import factors.IProbabilisticTransitionFormula;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.Effect;
import mdp.EffectClass;
import mdp.FormulaActionDescription;
import mdp.IActionDescription;
import mdp.Precondition;
import mdp.ProbabilisticEffect;
import mdp.ProbabilisticTransition;

/**
 * {@link RobotBumpedActionDescription} is an action description for the "rBumped" effect class of an instance of
 * {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public class RobotBumpedActionDescription implements IActionDescription<MoveToAction> {

	private static final double BUMP_PROB_PARTIALLY_OCCLUDED = 0.2;
	private static final double BUMP_PROB_BLOCKED = 1.0;
	private static final double BUMP_PROB_CLEAR = 0.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocSrcDef;
	private StateVarDefinition<RobotBumped> mrBumpedDestDef;
	private FormulaActionDescription<MoveToAction> mrBumpedActionDesc;

	public RobotBumpedActionDescription(ActionDefinition<MoveToAction> moveToDef,
			Precondition<MoveToAction> precondition, StateVarDefinition<Location> rLocSrcDef,
			StateVarDefinition<RobotBumped> rBumpedDestDef) {
		mrLocSrcDef = rLocSrcDef;
		mrBumpedDestDef = rBumpedDestDef;
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(rLocSrcDef);
		EffectClass effectClass = new EffectClass();
		effectClass.add(rBumpedDestDef);
		IProbabilisticTransitionFormula<MoveToAction> probTransFormula = getRobotBumpedFormula();
		mrBumpedActionDesc = new FormulaActionDescription<>(moveToDef, discrClass, effectClass, probTransFormula);
		initialize(moveToDef, precondition);
	}

	private IProbabilisticTransitionFormula<MoveToAction> getRobotBumpedFormula() {
		return null;
	}

	private void initialize(ActionDefinition<MoveToAction> moveToDef, Precondition<MoveToAction> precondition)
			throws ActionNotFoundException, IncompatibleVarException, AttributeNameNotFoundException,
			IncompatibleEffectClassException, IncompatibleActionException, IncompatibleDiscriminantClassException {
		DiscriminantClass rBumpedDiscrClass = mrBumpedActionDesc.getDiscriminantClass();
		EffectClass rBumpedEffectClass = mrBumpedActionDesc.getEffectClass();

		for (MoveToAction moveTo : moveToDef.getActions()) {
			Set<Location> applicableLocs = precondition.getApplicableValues(moveTo, mrLocSrcDef);

			for (Location rLocSrcValue : applicableLocs) {
				StateVar<Location> rLocSrc = mrLocSrcDef.getStateVar(rLocSrcValue);
				Discriminant rLocDiscriminant = new Discriminant(rBumpedDiscrClass);
				rLocDiscriminant.add(rLocSrc);

				ProbabilisticEffect rBumpedProbEffect = new ProbabilisticEffect(rBumpedEffectClass);
				Effect bumpedEffect = new Effect(rBumpedEffectClass);
				Effect notBumpedEffect = new Effect(rBumpedEffectClass);
				StateVar<RobotBumped> bumped = mrBumpedDestDef.getStateVar(new RobotBumped(true));
				StateVar<RobotBumped> notBumped = mrBumpedDestDef.getStateVar(new RobotBumped(false));
				bumpedEffect.add(bumped);
				notBumpedEffect.add(notBumped);

				Occlusion occlusion = moveTo.getOcclusion(rLocSrc);
				if (occlusion == Occlusion.PARTIALLY_OCCLUDED) {
					rBumpedProbEffect.put(bumpedEffect, BUMP_PROB_PARTIALLY_OCCLUDED);
					rBumpedProbEffect.put(notBumpedEffect, 1 - BUMP_PROB_PARTIALLY_OCCLUDED);
				} else if (occlusion == Occlusion.BLOCKED) {
					rBumpedProbEffect.put(bumpedEffect, BUMP_PROB_BLOCKED);
				} else {
					rBumpedProbEffect.put(notBumpedEffect, 1 - BUMP_PROB_CLEAR);
				}
				mrBumpedActionDesc.put(rBumpedProbEffect, rLocDiscriminant, moveTo);
			}
		}
	}

	/**
	 * 
	 * @param rBumpedDest
	 * @param rLocSrc
	 * @param moveTo
	 * @return Pr(rBumped' | rLoc, moveTo(L))
	 * @throws ActionNotFoundException
	 * @throws DiscriminantNotFoundException
	 * @throws EffectNotFoundException
	 * @throws IncompatibleVarException
	 */
	public double getProbability(StateVar<RobotBumped> rBumpedDest, StateVar<Location> rLocSrc, MoveToAction moveTo)
			throws ActionNotFoundException, DiscriminantNotFoundException, EffectNotFoundException,
			IncompatibleVarException {
		Effect rBumpedEffect = new Effect(getEffectClass());
		rBumpedEffect.add(rBumpedDest);
		Discriminant rLocDiscriminant = new Discriminant(getDiscriminantClass());
		rLocDiscriminant.add(rLocSrc);
		return mrBumpedActionDesc.getProbability(rBumpedEffect, rLocDiscriminant, moveTo);
	}

	@Override
	public Set<ProbabilisticTransition<MoveToAction>> getProbabilisticTransitions(MoveToAction action)
			throws ActionNotFoundException {
		return mrBumpedActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public double getProbability(Effect effect, Discriminant discriminant, MoveToAction moveTo)
			throws ActionNotFoundException, DiscriminantNotFoundException, EffectNotFoundException {
		return mrBumpedActionDesc.getProbability(effect, discriminant, moveTo);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, MoveToAction moveTo)
			throws ActionNotFoundException, DiscriminantNotFoundException {
		return mrBumpedActionDesc.getProbabilisticEffect(discriminant, moveTo);
	}

	@Override
	public ActionDefinition<MoveToAction> getActionDefinition() {
		return mrBumpedActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mrBumpedActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mrBumpedActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RobotBumpedActionDescription)) {
			return false;
		}
		RobotBumpedActionDescription actionDesc = (RobotBumpedActionDescription) obj;
		return actionDesc.mrBumpedActionDesc.equals(mrBumpedActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrBumpedActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
