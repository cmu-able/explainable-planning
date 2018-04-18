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
import mdp.ProbabilisticTransition;

/**
 * {@link RobotLocationActionDescription} is an action description for the "rLoc" effect class of an instance of
 * {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public class RobotLocationActionDescription implements IActionDescription<MoveToAction> {

	private static final double MOVE_PROB_NONBLOCKED = 1.0;
	private static final double MOVE_PROB_BLOCKED = 0.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<Location> mrLocDef;
	private ActionDescription<MoveToAction> mrLocActionDesc;

	public RobotLocationActionDescription(ActionDefinition<MoveToAction> moveToDef,
			StateVarDefinition<Location> rLocDef) {
		mrLocDef = rLocDef;
		mrLocActionDesc = new ActionDescription<>(moveToDef);
		mrLocActionDesc.addDiscriminantVarDef(rLocDef);
		mrLocActionDesc.addEffectVarDef(rLocDef);
	}

	public void put(MoveToAction moveTo, Precondition precondition)
			throws IncompatibleVarException, AttributeNameNotFoundException, IncompatibleEffectClassException,
			IncompatibleDiscriminantClassException, IncompatibleActionException {
		DiscriminantClass rLocDiscrClass = mrLocActionDesc.getDiscriminantClass();
		EffectClass rLocEffectClass = mrLocActionDesc.getEffectClass();
		Set<Location> applicableLocs = precondition.getApplicableValues(mrLocDef);
		for (Location rLocSrcValue : applicableLocs) {
			StateVar<Location> rLocSrc = new StateVar<>(mrLocDef, rLocSrcValue);
			Discriminant rLocDiscriminant = new Discriminant(rLocDiscrClass);
			rLocDiscriminant.add(rLocSrc);

			ProbabilisticEffect rLocProbEffect = new ProbabilisticEffect(rLocEffectClass);
			Effect newLocEffect = new Effect(rLocEffectClass);
			Effect oldLocEffect = new Effect(rLocEffectClass);
			StateVar<Location> newLoc = new StateVar<>(mrLocDef, moveTo.getDestination());
			StateVar<Location> oldLoc = new StateVar<>(mrLocDef, rLocSrc.getValue());
			newLocEffect.add(newLoc);
			oldLocEffect.add(oldLoc);

			Occlusion occlusion = moveTo.getOcclusion(rLocSrc);
			if (occlusion == Occlusion.BLOCKED) {
				rLocProbEffect.put(oldLocEffect, 1 - MOVE_PROB_BLOCKED);
			} else {
				rLocProbEffect.put(newLocEffect, MOVE_PROB_NONBLOCKED);
			}
			mrLocActionDesc.put(rLocProbEffect, rLocDiscriminant, moveTo);
		}
	}

	/**
	 * 
	 * @param rLocDest
	 * @param rLocSrc
	 * @param moveTo
	 * @return Pr(rLoc' | rLoc, moveTo(L))
	 * @throws ActionNotFoundException
	 * @throws DiscriminantNotFoundException
	 * @throws EffectNotFoundException
	 * @throws IncompatibleVarException
	 */
	public double getProbability(StateVar<Location> rLocDest, StateVar<Location> rLocSrc, MoveToAction moveTo)
			throws ActionNotFoundException, DiscriminantNotFoundException, EffectNotFoundException,
			IncompatibleVarException {
		Effect rLocEffect = new Effect(getEffectClass());
		rLocEffect.add(rLocDest);
		Discriminant rLocDiscriminant = new Discriminant(getDiscriminantClass());
		rLocDiscriminant.add(rLocSrc);
		return mrLocActionDesc.getProbability(rLocEffect, rLocDiscriminant, moveTo);
	}

	@Override
	public Set<ProbabilisticTransition<MoveToAction>> getProbabilisticTransitions(MoveToAction action)
			throws ActionNotFoundException {
		return mrLocActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public double getProbability(Effect effect, Discriminant discriminant, MoveToAction moveTo)
			throws ActionNotFoundException, DiscriminantNotFoundException, EffectNotFoundException {
		return mrLocActionDesc.getProbability(effect, discriminant, moveTo);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, MoveToAction moveTo)
			throws ActionNotFoundException, DiscriminantNotFoundException {
		return mrLocActionDesc.getProbabilisticEffect(discriminant, moveTo);
	}

	@Override
	public ActionDefinition<MoveToAction> getActionDefinition() {
		return mrLocActionDesc.getActionDefinition();
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
