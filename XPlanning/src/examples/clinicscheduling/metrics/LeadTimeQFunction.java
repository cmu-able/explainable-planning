package examples.clinicscheduling.metrics;

import examples.clinicscheduling.models.ClientCount;
import examples.clinicscheduling.models.ScheduleAction;
import language.domain.metrics.IStandardMetricQFunction;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

public class LeadTimeQFunction implements IStandardMetricQFunction<ScheduleAction, LeadTimeDomain> {

	public static final String NAME = "lead-time";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private LeadTimeDomain mDomain;
	private double mLeadTimeCostFactor;

	public LeadTimeQFunction(LeadTimeDomain domain, double leadTimeCostFactor) {
		mDomain = domain;
		mLeadTimeCostFactor = leadTimeCostFactor;
	}

	public double getLeadTimeCostFactor() {
		return mLeadTimeCostFactor;
	}

	/**
	 * Lead time cost = LT * x, where:
	 * 
	 * LT = lead time cost factor,
	 * 
	 * x = number of patients who have been booked (size of the queue).
	 */
	@Override
	public double getValue(Transition<ScheduleAction, LeadTimeDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		ClientCount bookedClientCount = mDomain.getCurrentBookedClientCount(transition);
		int x = bookedClientCount.getValue();
		return mLeadTimeCostFactor * x;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public LeadTimeDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof LeadTimeQFunction)) {
			return false;
		}
		LeadTimeQFunction qFunction = (LeadTimeQFunction) obj;
		return qFunction.mDomain.equals(mDomain)
				&& Double.compare(qFunction.mLeadTimeCostFactor, mLeadTimeCostFactor) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + Double.hashCode(mLeadTimeCostFactor);
			hashCode = result;
		}
		return result;
	}

}
