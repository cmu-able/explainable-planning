package examples.clinicscheduling.metrics;

import examples.clinicscheduling.models.ABP;
import examples.clinicscheduling.models.ClientCount;
import examples.clinicscheduling.models.ScheduleAction;
import language.domain.metrics.IStandardMetricQFunction;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

public class RevenueQFunction implements IStandardMetricQFunction<ScheduleAction, ScheduleDomain> {

	public static final String NAME = "revenue";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ScheduleDomain mDomain;
	private double mRevenuePerPatient;

	public RevenueQFunction(ScheduleDomain domain, double revenuePerPatient) {
		mDomain = domain;
		mRevenuePerPatient = revenuePerPatient;
	}

	public double getRevenuePerPatient() {
		return mRevenuePerPatient;
	}

	/**
	 * Revenue = R * (p_s(w,x) * min(w,x) + p_sd * b), where:
	 * 
	 * R = revenue per patient serviced,
	 * 
	 * w = current ABP,
	 * 
	 * x = number of patients who have been booked,
	 * 
	 * p_s(w,x) = probability that an advance-booking patient shows up, given x and w,
	 * 
	 * p_sd = probability that a same-day booking patient shows up.
	 */
	@Override
	public double getValue(Transition<ScheduleAction, ScheduleDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		ClientCount bookedClientCount = mDomain.getCurrentBookedClientCount(transition);
		ABP abp = mDomain.getCurrentABP(transition);
		double advanceBookingShowProb = ClientPredictionUtils.getAdvanceBookingShowProbability(bookedClientCount, abp);
		double sameDayShowProb = ClientPredictionUtils.getSameDayShowProbability();
		int w = abp.getValue();
		int x = bookedClientCount.getValue();
		int b = mDomain.getNumNewClientsToService(transition).getValue();
		return mRevenuePerPatient * (advanceBookingShowProb * Math.min(w, x) + sameDayShowProb * b);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public ScheduleDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RevenueQFunction)) {
			return false;
		}
		RevenueQFunction qFunction = (RevenueQFunction) obj;
		return qFunction.mDomain.equals(mDomain)
				&& Double.compare(qFunction.mRevenuePerPatient, mRevenuePerPatient) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + Double.hashCode(mRevenuePerPatient);
			hashCode = result;
		}
		return result;
	}

}
