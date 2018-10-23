package examples.clinicscheduling.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.ArithmeticUtils;

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
	 * Create a set of possible numbers of new clients arriving today.
	 * 
	 * @param clientArrivalRate
	 *            : Average client arrival rate
	 * @param branchFactor
	 *            : Number of sampled numbers of new clients
	 * @return A set of possible numbers of new clients arriving today
	 */
	public static Set<ClientCount> getPossibleNewClientCounts(double clientArrivalRate, int branchFactor) {
		Set<ClientCount> possibleNewClientCounts = new HashSet<>();

		// Branching factor is always an odd number
		double interval = clientArrivalRate / (Math.floorDiv(branchFactor - 1, 2) + 1);

		// At the end of each interval, pick the value as a possible number of new clients
		for (int i = 1; i <= branchFactor; i++) {
			int numClients = (int) Math.round(i * interval);

			ClientCount clientCount = new ClientCount(numClients);
			possibleNewClientCounts.add(clientCount);
		}

		return possibleNewClientCounts;
	}

	/**
	 * Calculate a discrete probability distribution of a number of new clients arriving today.
	 * 
	 * @param clientArrivalRate
	 *            : Average client arrival rate
	 * @param branchFactor
	 *            : Number of sampled numbers of new clients
	 * @return Probability distribution of a number of new clients arriving today
	 */
	public static Map<ClientCount, Double> getNewClientCountDistribution(double clientArrivalRate, int branchFactor) {
		// Branching factor is always an odd number
		double interval = clientArrivalRate / (Math.floorDiv(branchFactor - 1, 2) + 1);

		// Sampled possible numbers of new clients
		Set<ClientCount> possibleNewClientCounts = getPossibleNewClientCounts(clientArrivalRate, branchFactor);

		List<ClientCount> sortedPossibleNewClientCounts = new ArrayList<>(possibleNewClientCounts);
		Collections.sort(sortedPossibleNewClientCounts);

		Map<ClientCount, Double> distribution = new HashMap<>();
		double cumulativeProb = 0;

		for (int i = 0; i < sortedPossibleNewClientCounts.size(); i++) {
			ClientCount newClientCount = sortedPossibleNewClientCounts.get(i);
			double rangeProb;

			if (i == 0) {
				double lowerBound = 0;
				double upperBound = newClientCount.getValue() + 0.5 * interval;
				rangeProb = getNewClientCountRangeProbability(lowerBound, upperBound, clientArrivalRate);
			} else if (i == sortedPossibleNewClientCounts.size() - 1) {
				rangeProb = 1 - cumulativeProb;
			} else {
				double lowerBound = newClientCount.getValue() - 0.5 * interval;
				double upperBound = newClientCount.getValue() + 0.5 * interval;
				rangeProb = getNewClientCountRangeProbability(lowerBound, upperBound, clientArrivalRate);
			}

			distribution.put(newClientCount, rangeProb);
			cumulativeProb += rangeProb;
		}
		return distribution;
	}

	/**
	 * Calculate the total probability of having a number of new clients within a given range [lower, upper).
	 * 
	 * @param lowerBound
	 *            : Inclusive lower bound of number of new clients
	 * @param upperBound
	 *            : Exclusive upper bound of number of new clients
	 * @param clientArrivalRate
	 * @return Total probability of having a number of new clients within the given range
	 */
	private static double getNewClientCountRangeProbability(double lowerBound, double upperBound,
			double clientArrivalRate) {
		int roundedLowerBound = (int) Math.round(lowerBound);
		int roundedUpperBound = (int) Math.round(upperBound);
		double rangeProbability = 0;
		for (int k = roundedLowerBound; k < roundedUpperBound; k++) {
			// Probability of each number of new clients arriving today
			// Poisson distribution: P(k events in a day) = e^(-lambda) * lambda^k / k!
			double probNumNewClients = Math.pow(Math.E, -1 * clientArrivalRate) * Math.pow(clientArrivalRate, k)
					/ ArithmeticUtils.factorial(k);
			rangeProbability += probNumNewClients;

		}
		return rangeProbability;
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
