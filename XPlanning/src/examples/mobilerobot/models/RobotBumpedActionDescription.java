package examples.mobilerobot.models;

import java.util.Set;

import language.domain.models.ActionDefinition;
import language.domain.models.StateVarDefinition;
import language.exceptions.XMDPException;
import language.mdp.Discriminant;
import language.mdp.DiscriminantClass;
import language.mdp.EffectClass;
import language.mdp.FormulaActionDescription;
import language.mdp.IActionDescription;
import language.mdp.Precondition;
import language.mdp.ProbabilisticEffect;
import language.mdp.ProbabilisticTransition;

/**
 * {@link RobotBumpedActionDescription} is an action description for the "rBumped" effect class of an instance of
 * {@link MoveToAction}. It uses a {@link FormulaActionDescription}.
 * 
 * In the future, the constructor of this type may read an input formula for the "rBumped" effect and create a
 * {@link RobotBumpedFormula} accordingly.
 * 
 * @author rsukkerd
 *
 */
public class RobotBumpedActionDescription implements IActionDescription<MoveToAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<MoveToAction> mrBumpedActionDesc;

	public RobotBumpedActionDescription(ActionDefinition<MoveToAction> moveToDef,
			Precondition<MoveToAction> precondition, StateVarDefinition<Location> rLocSrcDef,
			StateVarDefinition<RobotBumped> rBumpedDestDef) {
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(rLocSrcDef);
		EffectClass effectClass = new EffectClass();
		effectClass.add(rBumpedDestDef);
		RobotBumpedFormula rBumpedFormula = new RobotBumpedFormula(rLocSrcDef, rBumpedDestDef);
		mrBumpedActionDesc = new FormulaActionDescription<>(moveToDef, precondition, discrClass, effectClass,
				rBumpedFormula);
	}

	@Override
	public Set<ProbabilisticTransition<MoveToAction>> getProbabilisticTransitions(MoveToAction action)
			throws XMDPException {
		return mrBumpedActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, MoveToAction moveTo)
			throws XMDPException {
		return mrBumpedActionDesc.getProbabilisticEffect(discriminant, moveTo);
	}

	@Override
	public ActionDefinition<MoveToAction> getActionDefinition() {
		return mrBumpedActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mrBumpedActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mrBumpedActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RobotBumpedActionDescription)) {
			return false;
		}
		RobotBumpedActionDescription actionDesc = (RobotBumpedActionDescription) obj;
		return actionDesc.mrBumpedActionDesc.equals(mrBumpedActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mrBumpedActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
