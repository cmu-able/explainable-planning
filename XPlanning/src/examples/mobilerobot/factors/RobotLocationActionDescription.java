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
 * {@link RobotLocationActionDescription} is an action description for the "rLoc" effect class of an instance of
 * {@link MoveToAction}.
 * 
 * @author rsukkerd
 *
 */
public class RobotLocationActionDescription implements IActionDescription {

	private static final double MOVE_PROB_NONBLOCKED = 1.0;
	private static final double MOVE_PROB_BLOCKED = 0.0;

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
			Set<StateVar<IStateVarValue>> newLocEffect = new HashSet<>();
			Set<StateVar<IStateVarValue>> oldLocEffect = new HashSet<>();
			StateVar<IStateVarValue> newLoc = new StateVar<>("rLoc", moveTo.getDestination());
			StateVar<IStateVarValue> oldLoc = new StateVar<>("rLoc", rLocSrc.getValue());
			newLocEffect.add(newLoc);
			oldLocEffect.add(oldLoc);

			Occlusion occlusion = moveTo.getOcclusion(rLocSrc);
			if (occlusion == Occlusion.BLOCKED) {
				rLocProbEffect.put(oldLocEffect, 1 - MOVE_PROB_BLOCKED);
				rLocProbEffect.put(newLocEffect, MOVE_PROB_BLOCKED);
			} else {
				rLocProbEffect.put(oldLocEffect, 1 - MOVE_PROB_NONBLOCKED);
				rLocProbEffect.put(newLocEffect, MOVE_PROB_NONBLOCKED);
			}
			mrLocActionDesc.put(rLocDiscriminant, rLocProbEffect);
		}
	}

	@Override
	public Iterator<Entry<Discriminant, ProbabilisticEffect>> iterator() {
		return mrLocActionDesc.iterator();
	}

	@Override
	public EffectClass getEffectClass() {
		return mrLocActionDesc.getEffectClass();
	}

}
