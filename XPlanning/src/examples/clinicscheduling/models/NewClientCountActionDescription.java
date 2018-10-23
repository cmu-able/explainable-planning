package examples.clinicscheduling.models;

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

public class NewClientCountActionDescription implements IActionDescription<ScheduleAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<ScheduleAction> mNewClientCountActionDesc;

	public NewClientCountActionDescription(ActionDefinition<ScheduleAction> scheduleDef,
			Precondition<ScheduleAction> precondition, StateVarDefinition<ClientCount> newClientCountDef,
			double clientArrivalRate, int branchFactor) {
		DiscriminantClass discrClass = new DiscriminantClass(); // empty discriminant class
		EffectClass effectClass = new EffectClass();
		effectClass.add(newClientCountDef);
		NewClientCountFormula newClientCountFormula = new NewClientCountFormula(newClientCountDef, clientArrivalRate,
				branchFactor);
		mNewClientCountActionDesc = new FormulaActionDescription<>(scheduleDef, precondition, discrClass, effectClass,
				newClientCountFormula);
	}

	@Override
	public Set<ProbabilisticTransition<ScheduleAction>> getProbabilisticTransitions(ScheduleAction action)
			throws XMDPException {
		return mNewClientCountActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, ScheduleAction action)
			throws XMDPException {
		return mNewClientCountActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<ScheduleAction> getActionDefinition() {
		return mNewClientCountActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mNewClientCountActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mNewClientCountActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NewClientCountActionDescription)) {
			return false;
		}
		NewClientCountActionDescription actionDesc = (NewClientCountActionDescription) obj;
		return actionDesc.mNewClientCountActionDesc.equals(mNewClientCountActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mNewClientCountActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
