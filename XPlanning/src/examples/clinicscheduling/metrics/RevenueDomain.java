package examples.clinicscheduling.metrics;

import java.util.Set;

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

public class RevenueDomain implements ITransitionStructure<ScheduleAction> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarDefinition<ABP> mABPSrcDef;
	private StateVarDefinition<ClientCount> mBookedClientCountSrcDef;

	private TransitionStructure<ScheduleAction> mDomain = new TransitionStructure<>();

	public RevenueDomain(StateVarDefinition<ABP> abpSrcDef, StateVarDefinition<ClientCount> bookedClientCountSrcDef,
			ActionDefinition<ScheduleAction> scheduleDef) {
		mABPSrcDef = abpSrcDef;
		mBookedClientCountSrcDef = bookedClientCountSrcDef;

		mDomain.addSrcStateVarDef(abpSrcDef);
		mDomain.addSrcStateVarDef(bookedClientCountSrcDef);
		mDomain.setActionDef(scheduleDef);
	}

	public ABP getCurrentABP(Transition<ScheduleAction, RevenueDomain> transition) throws VarNotFoundException {
		return transition.getSrcStateVarValue(ABP.class, mABPSrcDef);
	}

	public ClientCount getCurrentBookedClientCount(Transition<ScheduleAction, RevenueDomain> transition)
			throws VarNotFoundException {
		return transition.getSrcStateVarValue(ClientCount.class, mBookedClientCountSrcDef);
	}

	public ClientCount getNumNewClientsToService(Transition<ScheduleAction, RevenueDomain> transition) {
		return transition.getAction().getNumNewClientsToService();
	}

	@Override
	public Set<StateVarDefinition<IStateVarValue>> getSrcStateVarDefs() {
		return mDomain.getSrcStateVarDefs();
	}

	@Override
	public Set<StateVarDefinition<IStateVarValue>> getDestStateVarDefs() {
		return mDomain.getDestStateVarDefs();
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
		if (!(obj instanceof RevenueDomain)) {
			return false;
		}
		RevenueDomain domain = (RevenueDomain) obj;
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
