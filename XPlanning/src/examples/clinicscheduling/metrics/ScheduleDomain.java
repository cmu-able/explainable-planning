package examples.clinicscheduling.metrics;

import examples.clinicscheduling.models.ABP;
import examples.clinicscheduling.models.ClientCount;
import examples.clinicscheduling.models.ScheduleAction;
import language.domain.metrics.ITransitionStructure;
import language.domain.metrics.Transition;
import language.domain.metrics.TransitionStructure;
import language.domain.models.ActionDefinition;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;
import language.exceptions.VarNotFoundException;
import language.mdp.StateVarClass;

public class ScheduleDomain implements ITransitionStructure<ScheduleAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<ABP> mABPSrcDef;
	private StateVarDefinition<ClientCount> mBookedClientCountSrcDef;

	private TransitionStructure<ScheduleAction> mDomain = new TransitionStructure<>();

	public ScheduleDomain(StateVarDefinition<ABP> abpSrcDef, StateVarDefinition<ClientCount> bookedClientCountSrcDef,
			ActionDefinition<ScheduleAction> scheduleDef) {
		mABPSrcDef = abpSrcDef;
		mBookedClientCountSrcDef = bookedClientCountSrcDef;

		mDomain.addSrcStateVarDef(abpSrcDef);
		mDomain.addSrcStateVarDef(bookedClientCountSrcDef);
		mDomain.setActionDef(scheduleDef);
	}

	public ABP getCurrentABP(Transition<ScheduleAction, ScheduleDomain> transition) throws VarNotFoundException {
		return transition.getSrcStateVarValue(ABP.class, mABPSrcDef);
	}

	public ClientCount getCurrentBookedClientCount(Transition<ScheduleAction, ScheduleDomain> transition)
			throws VarNotFoundException {
		return transition.getSrcStateVarValue(ClientCount.class, mBookedClientCountSrcDef);
	}

	public ClientCount getNumNewClientsToService(Transition<ScheduleAction, ScheduleDomain> transition) {
		return transition.getAction().getNumNewClientsToService();
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
		if (!(obj instanceof ScheduleDomain)) {
			return false;
		}
		ScheduleDomain domain = (ScheduleDomain) obj;
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
