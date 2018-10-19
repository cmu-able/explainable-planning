package examples.clinicscheduling.metrics;

import examples.clinicscheduling.models.ABP;
import examples.clinicscheduling.models.ScheduleAction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.Transition;
import language.domain.metrics.TransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarClass;

public class SwitchABPDomain implements ITransitionStructure<ScheduleAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<ABP> mCurrentABP;

	private TransitionStructure<ScheduleAction> mDomain = new TransitionStructure<>();

	public SwitchABPDomain(StateVarDefinition<ABP> currentABP, ActionDefinition<ScheduleAction> scheduleDef) {
		mCurrentABP = currentABP;

		mDomain.addSrcStateVarDef(currentABP);
		mDomain.setActionDef(scheduleDef);
	}

	public ABP getCurrentABP(Transition<ScheduleAction, SwitchABPDomain> transition) throws VarNotFoundException {
		return transition.getSrcStateVarValue(ABP.class, mCurrentABP);
	}

	public ABP getNewABP(Transition<ScheduleAction, SwitchABPDomain> transition) {
		ScheduleAction schedule = transition.getAction();
		return schedule.getNewABP();
	}

	@Override
	public StateVarClass getSrcStateVarClass() {
		return mDomain.getSrcStateVarClass();
	}

	@Override
	public StateVarClass getDestStateVarClass() {
		return mDomain.getDestStateVarClass();
	}

	@Override
	public ActionDefinition<ScheduleAction> getActionDef() {
		return mDomain.getActionDef();
	}

	@Override
	public boolean containsSrcStateVarDef(StateVarDefinition<? extends IStateVarValue> srcVarDef) {
		return mDomain.containsSrcStateVarDef(srcVarDef);
	}

	@Override
	public boolean containsDestStateVarDef(StateVarDefinition<? extends IStateVarValue> destVarDef) {
		return mDomain.containsDestStateVarDef(destVarDef);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof SwitchABPDomain)) {
			return false;
		}
		SwitchABPDomain domain = (SwitchABPDomain) obj;
		return domain.mDomain.equals(mDomain);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			hashCode = result;
		}
		return result;
	}

}
