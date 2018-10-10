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

	private static final double BETA_1 = 12;
	private static final double BETA_2 = 36.54;

	/**
	 * Lower bound on the show probability of any advance-booking client.
	 */
	private static final double BETA_3 = 0.5;

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

	/**
	 * Show probability of an advance-booking appointment: p_s(w,x) = max(1 - (B1 + B2 + log(LT + 1))/100, B3).
	 * 
	 * @param transition
	 * @return Show probability of an advance-booking appointment
	 * @throws VarNotFoundException
	 */
	public double getAdvanceBookingShowProbability(Transition<ScheduleAction, RevenueDomain> transition)
			throws VarNotFoundException {
		double leadTime = getAppointmentLeadTime(transition);
		double gallucciTerm = 1 - (BETA_1 + BETA_2 * Math.log(leadTime + 1)) / 100;
		return Math.max(gallucciTerm, BETA_3);
	}

	/**
	 * Show probability of a same-day appointment: p_sd = 1 - B1/100.
	 * 
	 * @return Show probability of a same-day appointment
	 */
	public double getSameDayShowProbability() {
		return 1 - BETA_1 / 100;
	}

	/**
	 * Lead time of advance-booking appointment: LT = max(1, floor(x/w)).
	 * 
	 * @param transition
	 * @return Lead time of advance-booking appointment
	 * @throws VarNotFoundException
	 */
	public double getAppointmentLeadTime(Transition<ScheduleAction, RevenueDomain> transition)
			throws VarNotFoundException {
		int x = getCurrentBookedClientCount(transition).getValue();
		int w = getCurrentABP(transition).getValue();
		return Math.max(1, Math.floorDiv(x, w));
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
