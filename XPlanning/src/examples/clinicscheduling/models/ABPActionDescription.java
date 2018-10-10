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

public class ABPActionDescription implements IActionDescription<ScheduleAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<ScheduleAction> mScheduleActionDesc;

	public ABPActionDescription(ActionDefinition<ScheduleAction> scheduleDef, Precondition<ScheduleAction> precondition,
			StateVarDefinition<ABP> abpDef) {
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(abpDef);
		EffectClass effectClass = new EffectClass();
		effectClass.add(abpDef);
		ABPFormula abpFormula = new ABPFormula(abpDef, precondition);
		mScheduleActionDesc = new FormulaActionDescription<>(scheduleDef, discrClass, effectClass, abpFormula);
	}

	@Override
	public Set<ProbabilisticTransition<ScheduleAction>> getProbabilisticTransitions(ScheduleAction action)
			throws XMDPException {
		return mScheduleActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, ScheduleAction action)
			throws XMDPException {
		return mScheduleActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<ScheduleAction> getActionDefinition() {
		return mScheduleActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mScheduleActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mScheduleActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ABPActionDescription)) {
			return false;
		}
		ABPActionDescription actionDesc = (ABPActionDescription) obj;
		return actionDesc.mScheduleActionDesc.equals(mScheduleActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mScheduleActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
