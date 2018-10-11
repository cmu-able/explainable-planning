package examples.clinicscheduling.metrics;

import examples.clinicscheduling.models.ABP;
import examples.clinicscheduling.models.ClientCount;
import examples.clinicscheduling.models.ScheduleAction;
import language.domain.metrics.IStandardMetricQFunction;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

public class OvertimeQFunction implements IStandardMetricQFunction<ScheduleAction, ScheduleDomain> {

	public static final String NAME = "overtime";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private ScheduleDomain mDomain;
	private double mOvertimeCostPerPatient;
	private int mCapacity;

	public OvertimeQFunction(ScheduleDomain domain, double overtimeCostPerPatient, int capacity) {
		mDomain = domain;
		mOvertimeCostPerPatient = overtimeCostPerPatient;
		mCapacity = capacity;
	}

	public double getOvertimeCostPerPatient() {
		return mOvertimeCostPerPatient;
	}

	/**
	 * Overtime cost = OT * sum_i,j (i + j - C)+ * Pr(AB = i) * Pr(SD = j), where:
	 * 
	 * OT = overtime cost per patient serviced,
	 * 
	 * i = from 0 to number of patients advanced-booked for today, min(w, x),
	 * 
	 * j = from 0 to number of newly arrived patients to service today, b,
	 * 
	 * C = capacity of the clinic,
	 * 
	 * Pr(AB = i) = probability of i advanced-booking patients show up:
	 * 
	 * Pr(AB = i) = p_s^i for all i > 0, (1 - p_s)^min(w, x) for i = 0,
	 * 
	 * Pr(SD = j) = probability of j same-day patients show up:
	 * 
	 * Pr(SD = j) = p_sd^j for all j > 0, (1 - p_sd)^b for i = 0.
	 */
	@Override
	public double getValue(Transition<ScheduleAction, ScheduleDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		ClientCount bookedClientCount = mDomain.getCurrentBookedClientCount(transition);
		ABP abp = mDomain.getCurrentABP(transition);
		int w = abp.getValue();
		int x = bookedClientCount.getValue();
		int b = mDomain.getNumNewClientsToService(transition).getValue();

		int numClientsBookedForToday = Math.min(w, x);
		double overtimeCost = 0;

		for (int i = 0; i <= numClientsBookedForToday; i++) {
			double iAdvanceBookingShowProb = ClientPredictionUtils.getAdvanceBookingShowProbability(i,
					bookedClientCount, abp);

			for (int j = 0; j <= b; j++) {
				double jSameDayShowProb = ClientPredictionUtils.getSameDayShowProbability(j, b);
				int numOvertimePatients = Math.max(i + j - mCapacity, 0);

				overtimeCost += numOvertimePatients * iAdvanceBookingShowProb * jSameDayShowProb;
			}
		}

		return mOvertimeCostPerPatient * overtimeCost;
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
		if (!(obj instanceof OvertimeQFunction)) {
			return false;
		}
		OvertimeQFunction qFunction = (OvertimeQFunction) obj;
		return qFunction.mDomain.equals(mDomain)
				&& Double.compare(qFunction.mOvertimeCostPerPatient, mOvertimeCostPerPatient) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + Double.hashCode(mOvertimeCostPerPatient);
			hashCode = result;
		}
		return result;
	}

}
