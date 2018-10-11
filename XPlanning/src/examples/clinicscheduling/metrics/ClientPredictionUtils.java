package examples.clinicscheduling.metrics;

import examples.clinicscheduling.models.ABP;
import examples.clinicscheduling.models.ClientCount;
import language.exceptions.VarNotFoundException;

public class ClientPredictionUtils {

	private static final double BETA_1 = 12;
	private static final double BETA_2 = 36.54;

	/**
	 * Lower bound on the show probability of any advance-booking client.
	 */
	private static final double BETA_3 = 0.5;

	private ClientPredictionUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Show probability of an advance-booking appointment: p_s(w,x) = max(1 - (B1 + B2 + log(LT + 1))/100, B3).
	 * 
	 * @param bookedClientCount
	 *            : Number of clients who have been booked
	 * @param abp
	 *            : Current ABP
	 * @return Show probability of an advance-booking appointment
	 * @throws VarNotFoundException
	 */
	public static double getAdvanceBookingShowProbability(ClientCount bookedClientCount, ABP abp) {
		double leadTime = getAppointmentLeadTime(bookedClientCount, abp);
		double gallucciTerm = 1 - (BETA_1 + BETA_2 * Math.log(leadTime + 1)) / 100;
		return Math.max(gallucciTerm, BETA_3);
	}

	/**
	 * Show probability of N advance-booking patients: Pr(AB = i) = p_s^i for all i > 0, (1 - p_s)^min(w, x) for i = 0.
	 * 
	 * @param numShowPatients
	 *            : Number of advance-booking patients who show up
	 * @param bookedClientCount
	 *            : Number of clients who have been booked
	 * @param abp
	 *            : Current ABP
	 * @return Show probability of N advance-booking patients
	 */
	public static double getAdvanceBookingShowProbability(int numShowPatients, ClientCount bookedClientCount, ABP abp) {
		int w = abp.getValue();
		int x = bookedClientCount.getValue();
		int numClientsBookedForToday = Math.min(w, x);
		double advanceBookingShowProb = getAdvanceBookingShowProbability(bookedClientCount, abp);
		return numShowPatients > 0 ? Math.pow(advanceBookingShowProb, numShowPatients)
				: Math.pow(1 - advanceBookingShowProb, numClientsBookedForToday);
	}

	/**
	 * Show probability of a same-day appointment: p_sd = 1 - B1/100.
	 * 
	 * @return Show probability of a same-day appointment
	 */
	public static double getSameDayShowProbability() {
		return 1 - BETA_1 / 100;
	}

	/**
	 * Show probability of N same-day patients: Pr(SD = j) = p_sd^j for all j > 0, (1 - p_sd)^b for i = 0.
	 * 
	 * @param numShowPatients
	 *            : Number of same-day patients who show up
	 * @param numNewClientsToService
	 *            : Number of newly arrived patients to service today
	 * @return Show probability of N same-day patients
	 */
	public static double getSameDayShowProbability(int numShowPatients, int numNewClientsToService) {
		double sameDayShowProb = getSameDayShowProbability();
		return numShowPatients > 0 ? Math.pow(sameDayShowProb, numShowPatients)
				: Math.pow(1 - sameDayShowProb, numNewClientsToService);
	}

	/**
	 * Lead time of advance-booking appointment: LT = max(1, floor(x/w)).
	 * 
	 * @param bookedClientCount
	 *            : Number of clients who have been booked
	 * @param abp
	 *            : Current ABP
	 * @return Lead time of advance-booking appointment
	 * @throws VarNotFoundException
	 */
	public static double getAppointmentLeadTime(ClientCount bookedClientCount, ABP abp) {
		int x = bookedClientCount.getValue();
		int w = abp.getValue();
		return Math.max(1, Math.floorDiv(x, w));
	}
}
