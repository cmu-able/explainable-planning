package examples.mobilerobot.factors;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;
import exceptions.IncompatibleDiscriminantClassException;
import exceptions.IncompatibleEffectClassException;
import exceptions.IncompatibleVarException;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.ActionDescription;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.Effect;
import mdp.EffectClass;
import mdp.IActionDescription;
import mdp.Precondition;
import mdp.ProbabilisticEffect;

/**
 * {@link RobotLocationActionDescription} is an action description for the "rLoc" effect class of an instance of
 * {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public class RobotLocationActionDescription implements IActionDescription {

	private static final double MOVE_PROB_NONBLOCKED = 1.0;
	private static final double MOVE_PROB_BLOCKED = 0.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDescription mrLocActionDesc;
	private MoveToAction mMoveTo;

	public RobotLocationActionDescription(MoveToAction moveTo, StateVarDefinition<Location> rLocDef,
			Precondition precondition) throws AttributeNameNotFoundException, IncompatibleVarException,
			IncompatibleEffectClassException, IncompatibleDiscriminantClassException {
		mMoveTo = moveTo;
		DiscriminantClass rLocDiscrClass = new DiscriminantClass(moveTo);
		rLocDiscrClass.add(rLocDef);
		EffectClass rLocEffectClass = new EffectClass(moveTo);
		rLocEffectClass.add(rLocDef);
		mrLocActionDesc = new ActionDescription(rLocDiscrClass, rLocEffectClass);

		Set<Location> applicableLocs = precondition.getApplicableValues(rLocDef);
		for (Location rLocSrcValue : applicableLocs) {
			StateVar<Location> rLocSrc = new StateVar<>(rLocDef, rLocSrcValue);
			Discriminant rLocDiscriminant = new Discriminant(rLocDiscrClass);
			rLocDiscriminant.add(rLocSrc);

			ProbabilisticEffect rLocProbEffect = new ProbabilisticEffect(rLocEffectClass);
			Effect newLocEffect = new Effect(rLocEffectClass);
			Effect oldLocEffect = new Effect(rLocEffectClass);
			StateVar<Location> newLoc = new StateVar<>(rLocDef, moveTo.getDestination());
			StateVar<Location> oldLoc = new StateVar<>(rLocDef, rLocSrc.getValue());
			newLocEffect.add(newLoc);
			oldLocEffect.add(oldLoc);

			Occlusion occlusion = moveTo.getOcclusion(rLocSrc);
			if (occlusion == Occlusion.BLOCKED) {
				rLocProbEffect.put(oldLocEffect, 1 - MOVE_PROB_BLOCKED);
			} else {
				rLocProbEffect.put(newLocEffect, MOVE_PROB_NONBLOCKED);
			}
			mrLocActionDesc.put(rLocDiscriminant, rLocProbEffect);
		}
	}

	public double getProbability(StateVar<Location> rLocDest, StateVar<Location> rLocSrc)
			throws DiscriminantNotFoundException, EffectNotFoundException, IncompatibleVarException {
		Effect rLocEffect = new Effect(mMoveTo, rLocDest.getDefinition());
		rLocEffect.add(rLocDest);
		Discriminant rLocDiscriminant = new Discriminant(mMoveTo, rLocSrc.getDefinition());
		rLocDiscriminant.add(rLocSrc);
		return mrLocActionDesc.getProbability(rLocEffect, rLocDiscriminant);
	}

	@Override
	public Iterator<Entry<Discriminant, ProbabilisticEffect>> iterator() {
		return mrLocActionDesc.iterator();
	}

	@Override
	public double getProbability(Effect effect, Discriminant discriminant)
			throws DiscriminantNotFoundException, EffectNotFoundException {
		return mrLocActionDesc.getProbability(effect, discriminant);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant) throws DiscriminantNotFoundException {
		return mrLocActionDesc.getProbabilisticEffect(discriminant);
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mrLocActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mrLocActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RobotLocationActionDescription)) {
			return false;
		}
		RobotLocationActionDescription actionDesc = (RobotLocationActionDescription) obj;
		return actionDesc.mrLocActionDesc.equals(mrLocActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrLocActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
