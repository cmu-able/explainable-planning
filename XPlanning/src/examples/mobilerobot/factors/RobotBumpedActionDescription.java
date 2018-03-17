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
 * {@link RobotBumpedActionDescription} is an action description for the "rBumped" effect class of a instance of
 * {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public class RobotBumpedActionDescription implements IActionDescription {

	private MoveToAction mMoveTo;
	private ActionDescription mrBumpedActionDesc;

	public RobotBumpedActionDescription(MoveToAction moveTo, Set<StateVar<Location>> applicablerLocSrcs,
			StateVarDefinition<RobotBumped> rBumpedDef) throws AttributeNameNotFoundException {
		mMoveTo = moveTo;
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

			if (isPartiallyOccluded(rLocSrc)) {
				rBumpedProbEffect.put(bumpedEffect, 0.2);
				rBumpedProbEffect.put(notBumpedEffect, 0.8);
			} else {
				rBumpedProbEffect.put(bumpedEffect, 0.0);
				rBumpedProbEffect.put(notBumpedEffect, 1.0);
			}
			mrBumpedActionDesc.put(rLocDiscriminant, rBumpedProbEffect);
		}
	}

	public boolean isPartiallyOccluded(StateVar<Location> rLocSrc) throws AttributeNameNotFoundException {
		return mMoveTo.getOcclusion(rLocSrc) == Occlusion.PARTIALLY_OCCLUDED;
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
