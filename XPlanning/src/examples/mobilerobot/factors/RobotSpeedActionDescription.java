package examples.mobilerobot.factors;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

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
 * {@link RobotSpeedActionDescription} is an action description for the "rSpeed" effect class of an instance of
 * {@link SetSpeedAction}.
 * 
 * @author rsukkerd
 *
 */
public class RobotSpeedActionDescription implements IActionDescription {

	private static final double SET_SPEED_PROB = 1.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDescription mrSpeedActionDesc;

	public RobotSpeedActionDescription(SetSpeedAction setSpeed, StateVarDefinition<RobotSpeed> rSpeedDef) {
		EffectClass rSpeedEffectClass = new EffectClass(setSpeed);
		rSpeedEffectClass.add(rSpeedDef);
		mrSpeedActionDesc = new ActionDescription(rSpeedEffectClass);

		Discriminant emptyDiscriminant = new Discriminant();
		ProbabilisticEffect rSpeedProbEffect = new ProbabilisticEffect();
		Effect newSpeedEffect = new Effect();
		StateVar<RobotSpeed> newSpeed = new StateVar<>(rSpeedDef.getName(), setSpeed.getTargetSpeed());
		newSpeedEffect.add(newSpeed);
		rSpeedProbEffect.put(newSpeedEffect, SET_SPEED_PROB);
		mrSpeedActionDesc.put(emptyDiscriminant, rSpeedProbEffect);
	}

	public double getProbability(StateVar<RobotSpeed> rSpeedDest)
			throws DiscriminantNotFoundException, EffectNotFoundException {
		Effect rSpeedEffect = new Effect();
		rSpeedEffect.add(rSpeedDest);
		Discriminant rSpeedDiscriminant = new Discriminant();
		return mrSpeedActionDesc.getProbability(rSpeedEffect, rSpeedDiscriminant);
	}

	@Override
	public Iterator<Entry<Discriminant, ProbabilisticEffect>> iterator() {
		return mrSpeedActionDesc.iterator();
	}

	@Override
	public double getProbability(Effect effect, Discriminant discriminant)
			throws DiscriminantNotFoundException, EffectNotFoundException {
		return mrSpeedActionDesc.getProbability(effect, discriminant);
	}

	@Override
	public Set<Effect> getPossibleEffects() {
		return mrSpeedActionDesc.getPossibleEffects();
	}

	@Override
	public EffectClass getEffectClass() {
		return mrSpeedActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RobotSpeedActionDescription)) {
			return false;
		}
		RobotSpeedActionDescription actionDesc = (RobotSpeedActionDescription) obj;
		return actionDesc.mrSpeedActionDesc.equals(mrSpeedActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrSpeedActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
