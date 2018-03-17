package examples.mobilerobot.factors;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import exceptions.AttributeNameNotFoundException;
import factors.IStateVarValue;
import factors.StateVar;
import factors.StateVarDefinition;
import mdp.ActionDescription;
import mdp.Discriminant;
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
	private ActionDescription mrBumpedActionDesc;

	public RobotBumpedActionDescription(MoveToAction moveTo, Set<StateVar<Location>> applicablerLocSrcs,
			StateVarDefinition<RobotBumped> rBumpedDef) throws AttributeNameNotFoundException {
		EffectClass rBumpedEffectClass = new EffectClass(moveTo);
		rBumpedEffectClass.add(rBumpedDef);
		mrBumpedActionDesc = new ActionDescription(rBumpedEffectClass);

		for (StateVar<Location> rLocSrc : applicablerLocSrcs) {
			StateVar<IStateVarValue> varSrc = new StateVar<>(rLocSrc.getName(), rLocSrc.getValue());
			Discriminant rLocDiscriminant = new Discriminant();
			rLocDiscriminant.add(varSrc);

			ProbabilisticEffect rBumpedProbEffect = new ProbabilisticEffect();
			Set<StateVar<IStateVarValue>> bumpedEffect = new HashSet<>();
			Set<StateVar<IStateVarValue>> notBumpedEffect = new HashSet<>();
			StateVar<IStateVarValue> bumped = new StateVar<>("rBumped", new RobotBumped(true));
			StateVar<IStateVarValue> notBumped = new StateVar<>("rBumped", new RobotBumped(false));
			bumpedEffect.add(bumped);
			notBumpedEffect.add(notBumped);

			Occlusion occlusion = moveTo.getOcclusion(rLocSrc);
			if (occlusion == Occlusion.PARTIALLY_OCCLUDED) {
				rBumpedProbEffect.put(bumpedEffect, BUMP_PROB_PARTIALLY_OCCLUDED);
				rBumpedProbEffect.put(notBumpedEffect, 1 - BUMP_PROB_PARTIALLY_OCCLUDED);
			} else if (occlusion == Occlusion.BLOCKED) {
				rBumpedProbEffect.put(bumpedEffect, BUMP_PROB_BLOCKED);
				rBumpedProbEffect.put(notBumpedEffect, 1 - BUMP_PROB_BLOCKED);
			} else {
				rBumpedProbEffect.put(bumpedEffect, BUMP_PROB_CLEAR);
				rBumpedProbEffect.put(notBumpedEffect, 1 - BUMP_PROB_CLEAR);
			}
			mrBumpedActionDesc.put(rLocDiscriminant, rBumpedProbEffect);
		}
	}

	@Override
	public Iterator<Entry<Discriminant, ProbabilisticEffect>> iterator() {
		return mrBumpedActionDesc.iterator();
	}

	@Override
	public EffectClass getEffectClass() {
		return mrBumpedActionDesc.getEffectClass();
	}

}
