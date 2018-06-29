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
		RobotSpeedFormula rSpeedFormula = new RobotSpeedFormula(rSpeedDef, precondition);
		mrSpeedActionDesc = new FormulaActionDescription<>(setSpeedDef, discrClass, effectClass, rSpeedFormula);
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
