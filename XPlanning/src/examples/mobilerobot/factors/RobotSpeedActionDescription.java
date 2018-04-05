package examples.mobilerobot.factors;

import java.util.Iterator;
import java.util.Map.Entry;

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
public class RobotSpeedActionDescription implements IActionDescription {

	private static final double SET_SPEED_PROB = 1.0;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ActionDescription mrSpeedActionDesc;
	private SetSpeedAction mSetSpeed;

	public RobotSpeedActionDescription(SetSpeedAction setSpeed, StateVarDefinition<RobotSpeed> rSpeedSrcDef)
			throws IncompatibleVarException, IncompatibleEffectClassException, IncompatibleDiscriminantClassException {
		mSetSpeed = setSpeed;
		DiscriminantClass emptyDiscrClass = new DiscriminantClass(setSpeed);
		EffectClass rSpeedEffectClass = new EffectClass(setSpeed);
		rSpeedEffectClass.add(rSpeedSrcDef);
		mrSpeedActionDesc = new ActionDescription(emptyDiscrClass, rSpeedEffectClass);

		Discriminant emptyDiscriminant = new Discriminant(emptyDiscrClass);
		ProbabilisticEffect rSpeedProbEffect = new ProbabilisticEffect(rSpeedEffectClass);
		Effect newSpeedEffect = new Effect(rSpeedEffectClass);
		StateVar<RobotSpeed> newSpeed = new StateVar<>(rSpeedSrcDef, setSpeed.getTargetSpeed());
		newSpeedEffect.add(newSpeed);
		rSpeedProbEffect.put(newSpeedEffect, SET_SPEED_PROB);
		mrSpeedActionDesc.put(emptyDiscriminant, rSpeedProbEffect);
	}

	public double getProbability(StateVar<RobotSpeed> rSpeedDest)
			throws DiscriminantNotFoundException, EffectNotFoundException, IncompatibleVarException {
		EffectClass rSpeedEffectClass = new EffectClass(mSetSpeed);
		rSpeedEffectClass.add(rSpeedDest.getDefinition());
		Effect rSpeedEffect = new Effect(rSpeedEffectClass);
		rSpeedEffect.add(rSpeedDest);
		DiscriminantClass emptyDiscrClass = new DiscriminantClass(mSetSpeed);
		Discriminant rSpeedDiscriminant = new Discriminant(emptyDiscrClass);
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
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant) throws DiscriminantNotFoundException {
		return mrSpeedActionDesc.getProbabilisticEffect(discriminant);
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
		return actionDesc.mrSpeedActionDesc.equals(mrSpeedActionDesc) && actionDesc.mSetSpeed.equals(mSetSpeed);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrSpeedActionDesc.hashCode();
			result = 31 * result + mSetSpeed.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
