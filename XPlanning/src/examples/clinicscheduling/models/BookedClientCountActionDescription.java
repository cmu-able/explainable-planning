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

public class BookedClientCountActionDescription implements IActionDescription<ScheduleAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private FormulaActionDescription<ScheduleAction> mBookedClientCountActionDesc;

	public BookedClientCountActionDescription(ActionDefinition<ScheduleAction> scheduleDef,
			Precondition<ScheduleAction> precondition, StateVarDefinition<ClientCount> bookedClientCountDef,
			StateVarDefinition<ABP> abpDef, StateVarDefinition<ClientCount> newClientCountDef) {
		DiscriminantClass discrClass = new DiscriminantClass();
		discrClass.add(bookedClientCountDef);
		discrClass.add(abpDef);
		discrClass.add(newClientCountDef);
		EffectClass effectClass = new EffectClass();
		effectClass.add(bookedClientCountDef);
		BookedClientCountFormula bookedClientFormula = new BookedClientCountFormula(bookedClientCountDef, abpDef,
				newClientCountDef);
		mBookedClientCountActionDesc = new FormulaActionDescription<>(scheduleDef, precondition, discrClass,
				effectClass, bookedClientFormula);
	}

	@Override
	public Set<ProbabilisticTransition<ScheduleAction>> getProbabilisticTransitions(ScheduleAction action)
			throws XMDPException {
		return mBookedClientCountActionDesc.getProbabilisticTransitions(action);
	}

	@Override
	public ProbabilisticEffect getProbabilisticEffect(Discriminant discriminant, ScheduleAction action)
			throws XMDPException {
		return mBookedClientCountActionDesc.getProbabilisticEffect(discriminant, action);
	}

	@Override
	public ActionDefinition<ScheduleAction> getActionDefinition() {
		return mBookedClientCountActionDesc.getActionDefinition();
	}

	@Override
	public DiscriminantClass getDiscriminantClass() {
		return mBookedClientCountActionDesc.getDiscriminantClass();
	}

	@Override
	public EffectClass getEffectClass() {
		return mBookedClientCountActionDesc.getEffectClass();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof BookedClientCountActionDescription)) {
			return false;
		}
		BookedClientCountActionDescription actionDesc = (BookedClientCountActionDescription) obj;
		return actionDesc.mBookedClientCountActionDesc.equals(mBookedClientCountActionDesc);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mBookedClientCountActionDesc.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

}
