package examples.mobilerobot.factors;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.AttributeNameNotFoundException;
import exceptions.DiscriminantNotFoundException;
import exceptions.EffectNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.ActionDescription;
import mdp.Discriminant;
import mdp.Effect;
import mdp.EffectClass;
import mdp.IActionDescription;
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

	public RobotLocationActionDescription(MoveToAction moveTo, Set<StateVar<Location>> applicablerLocSrcs,
			StateVarDefinition<Location> rLocDef) throws AttributeNameNotFoundException {
		EffectClass rLocEffectClass = new EffectClass(moveTo);
		rLocEffectClass.add(rLocDef);
		mrLocActionDesc = new ActionDescription(rLocEffectClass);

		for (StateVar<Location> rLocSrc : applicablerLocSrcs) {
			StateVar<IStateVarValue> varSrc = new StateVar<>(rLocSrc.getName(), rLocSrc.getValue());
			Discriminant rLocDiscriminant = new Discriminant();
			rLocDiscriminant.add(varSrc);

			ProbabilisticEffect rLocProbEffect = new ProbabilisticEffect();
			Effect newLocEffect = new Effect();
			Effect oldLocEffect = new Effect();
			StateVar<IStateVarValue> newLoc = new StateVar<>(rLocDef.getName(), moveTo.getDestination());
			StateVar<IStateVarValue> oldLoc = new StateVar<>(rLocDef.getName(), rLocSrc.getValue());
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
			throws DiscriminantNotFoundException, EffectNotFoundException {
		StateVar<IStateVarValue> varDest = new StateVar<>(rLocDest.getName(), rLocDest.getValue());
		StateVar<IStateVarValue> varSrc = new StateVar<>(rLocSrc.getName(), rLocSrc.getValue());
		Effect rLocEffect = new Effect();
		rLocEffect.add(varDest);
		Discriminant rLocDiscriminant = new Discriminant();
		rLocDiscriminant.add(varSrc);
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
