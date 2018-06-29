package examples.mobilerobot.factors;

import java.util.Set;

import exceptions.XMDPException;
import factors.ActionDefinition;
import factors.StateVarDefinition;
import mdp.Discriminant;
import mdp.DiscriminantClass;
import mdp.EffectClass;
import mdp.FormulaActionDescription;
import mdp.IActionDescription;
import mdp.Precondition;
import mdp.ProbabilisticEffect;
import mdp.ProbabilisticTransition;

/**
 * {@link RobotLocationActionDescription} is an action description for the "rLoc" effect class of an instance of
 * {@link MoveToAction}. It uses a {@link FormulaActionDescription}.
 * 
 * In the future, the constructor of this type may read an input formula for the "rLoc" effect and create a
 * {@link RobotLocationFormula} accordingly.
 * 
 * @author rsukkerd
 *
 */
public class RobotLocationActionDescription implements IActionDescription<MoveToAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<MoveToAction> mrLocActionDesc;

	public RobotLocationActionDescription(ActionDefinition<MoveToAction> moveToDef,
			Precondition<MoveToAction> precondition, StateVarDefinition<Location> rLocDef) {
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(rLocDef);
		EffectClass effectClass = new EffectClass();
		effectClass.add(rLocDef);
		RobotLocationFormula rLocFormula = new RobotLocationFormula(rLocDef, precondition);
		mrLocActionDesc = new FormulaActionDescription<>(moveToDef, discrClass, effectClass, rLocFormula);
	}

	@Override
	public Set<ProbabilisticTransition<MoveToAction>> getProbabilisticTransitions(MoveToAction action)
			throws XMDPException {
		return mrLocActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, MoveToAction moveTo)
			throws XMDPException {
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
