package examples.mobilerobot.factors;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.ActionDescription;
import mdp.Discriminant;
import mdp.Effect;
import mdp.EffectClass;
import mdp.IActionDescription;
import mdp.ProbabilisticEffect;

/**
 * {@link RobotBumpedActionDescription} is an action description for the "rBumped" effect class of an instance of
 * {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public class RobotBumpedActionDescription implements IActionDescription {

	private static final double BUMP_PROB_PARTIALLY_OCCLUDED = 0.2;
	private static final double BUMP_PROB_BLOCKED = 1.0;
	private static final double BUMP_PROB_CLEAR = 0.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDescription mrBumpedActionDesc;

	public RobotBumpedActionDescription(MoveToAction moveTo, Set<StateVar<Location>> applicablerLocSrcs,
			StateVarDefinition<RobotBumped> rBumpedDef) throws AttributeNameNotFoundException {
		EffectClass rBumpedEffectClass = new EffectClass(moveTo);
		rBumpedEffectClass.add(rBumpedDef);
		mrBumpedActionDesc = new ActionDescription(rBumpedEffectClass);

		for (StateVar<Location> rLocSrc : applicablerLocSrcs) {
			Discriminant rLocDiscriminant = new Discriminant();
			rLocDiscriminant.add(rLocSrc);

			ProbabilisticEffect rBumpedProbEffect = new ProbabilisticEffect();
			Effect bumpedEffect = new Effect();
			Effect notBumpedEffect = new Effect();
			StateVar<RobotBumped> bumped = new StateVar<>(rBumpedDef, new RobotBumped(true));
			StateVar<RobotBumped> notBumped = new StateVar<>(rBumpedDef, new RobotBumped(false));
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
			mrBumpedActionDesc.put(rLocDiscriminant, rBumpedProbEffect);
		}
	}

	public double getProbability(StateVar<RobotBumped> rBumpedDest, StateVar<Location> rLocSrc)
			throws DiscriminantNotFoundException, EffectNotFoundException {
		Effect rBumpedEffect = new Effect();
		rBumpedEffect.add(rBumpedDest);
		Discriminant rLocDiscriminant = new Discriminant();
		rLocDiscriminant.add(rLocSrc);
		return mrBumpedActionDesc.getProbability(rBumpedEffect, rLocDiscriminant);
	}

	@Override
	public Iterator<Entry<Discriminant, ProbabilisticEffect>> iterator() {
		return mrBumpedActionDesc.iterator();
	}

	@Override
	public double getProbability(Effect effect, Discriminant discriminant)
			throws DiscriminantNotFoundException, EffectNotFoundException {
		return mrBumpedActionDesc.getProbability(effect, discriminant);
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
