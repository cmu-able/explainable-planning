package examples.mobilerobot.factors;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import exceptions.ActionNotFoundException;
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
import mdp.ProbabilisticEffect;

/**
 * {@link RobotSpeedActionDescription} is an action description for the "rSpeed" effect class of an instance of
 * {@link SetSpeedAction}.
 * 
 * @author rsukkerd
 *
 */
public class RobotSpeedActionDescription implements IActionDescription<SetSpeedAction> {

	private static final double SET_SPEED_PROB = 1.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<RobotSpeed> mrSpeedDestDef;
	private ActionDescription<SetSpeedAction> mrSpeedActionDesc;

	public RobotSpeedActionDescription(StateVarDefinition<RobotSpeed> rSpeedDestDef) {
		mrSpeedDestDef = rSpeedDestDef;
		mrSpeedActionDesc = new ActionDescription<>();
		mrSpeedActionDesc.addEffectVarDef(rSpeedDestDef);
	}

	public void put(SetSpeedAction setSpeed)
			throws IncompatibleVarException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException {
		DiscriminantClass emptyDiscrClass = mrSpeedActionDesc.getDiscriminantClass();
		EffectClass rSpeedEffectClass = mrSpeedActionDesc.getEffectClass();
		Discriminant emptyDiscriminant = new Discriminant(emptyDiscrClass);
		ProbabilisticEffect rSpeedProbEffect = new ProbabilisticEffect(rSpeedEffectClass);
		Effect newSpeedEffect = new Effect(rSpeedEffectClass);
		StateVar<RobotSpeed> newSpeed = new StateVar<>(mrSpeedDestDef, setSpeed.getTargetSpeed());
		newSpeedEffect.add(newSpeed);
		rSpeedProbEffect.put(newSpeedEffect, SET_SPEED_PROB);
		mrSpeedActionDesc.put(rSpeedProbEffect, emptyDiscriminant, setSpeed);
	}

	public double getProbability(StateVar<RobotSpeed> rSpeedDest, SetSpeedAction setSpeed)
			throws ActionNotFoundException, DiscriminantNotFoundException, EffectNotFoundException,
			IncompatibleVarException {
		Effect rSpeedEffect = new Effect(getEffectClass());
		rSpeedEffect.add(rSpeedDest);
		Discriminant rSpeedDiscriminant = new Discriminant(getDiscriminantClass());
		return mrSpeedActionDesc.getProbability(rSpeedEffect, rSpeedDiscriminant, setSpeed);
	}

	@Override
	public Iterator<Entry<SetSpeedAction, Map<Discriminant, ProbabilisticEffect>>> iterator() {
		return mrSpeedActionDesc.iterator();
	}

	@Override
	public double getProbability(Effect effect, Discriminant discriminant, SetSpeedAction setSpeed)
			throws ActionNotFoundException, DiscriminantNotFoundException, EffectNotFoundException {
		return mrSpeedActionDesc.getProbability(effect, discriminant, setSpeed);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, SetSpeedAction setSpeed)
			throws ActionNotFoundException, DiscriminantNotFoundException {
		return mrSpeedActionDesc.getProbabilisticEffect(discriminant, setSpeed);
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mrSpeedActionDesc.getDiscriminantClass();
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
