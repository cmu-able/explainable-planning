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
 * {@link RobotSpeedActionDescription} is an action description for the "rSpeed" effect class of an instance of
 * {@link SetSpeedAction}.
 * 
 * @author rsukkerd
 *
 */
public class RobotSpeedActionDescription implements IActionDescription<SetSpeedAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<SetSpeedAction> mrSpeedActionDesc;

	public RobotSpeedActionDescription(ActionDefinition<SetSpeedAction> setSpeedDef,
			Precondition<SetSpeedAction> precondition, StateVarDefinition<RobotSpeed> rSpeedDef) {
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(rSpeedDef);
		EffectClass effectClass = new EffectClass();
		effectClass.add(rSpeedDef);
		RobotSpeedFormula rSpeedFormula = new RobotSpeedFormula(rSpeedDef);
		mrSpeedActionDesc = new FormulaActionDescription<>(setSpeedDef, precondition, discrClass, effectClass,
				rSpeedFormula);
	}

	@Override
	public Set<ProbabilisticTransition<SetSpeedAction>> getProbabilisticTransitions(SetSpeedAction action)
			throws XMDPException {
		return mrSpeedActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, SetSpeedAction setSpeed)
			throws XMDPException {
		return mrSpeedActionDesc.getProbabilisticEffect(discriminant, setSpeed);
	}

	@Override
	public ActionDefinition<SetSpeedAction> getActionDefinition() {
		return mrSpeedActionDesc.getActionDefinition();
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
