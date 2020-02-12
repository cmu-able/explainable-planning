package examples.dart.metrics;

import examples.dart.models.IDurativeAction;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamDestroyed;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamFormation;
import examples.dart.models.ThreatDistribution;
import language.domain.metrics.IStandardMetricQFunction;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

/**
 * {@link DestroyedProbabilityQFunction} represent the probability of the team being destroyed by a threat during
 * IncAlt/DecAlt/Fly action.
 * 
 * @author rsukkerd
 *
 */
public class DestroyedProbabilityQFunction
		implements IStandardMetricQFunction<IDurativeAction, DestroyedProbabilityDomain> {

	public static final String NAME = "destroyedProbability";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private DestroyedProbabilityDomain mDomain;
	private double mThreatRange; // at an altitude of r_T or higher, threats cannot shoot down the team
	private double mPsi; // factor by which the probability of being destroyed is reduced due to flying in tight
							// formation

	public DestroyedProbabilityQFunction(DestroyedProbabilityDomain domain, double threatRange, double psi) {
		mDomain = domain;
		mThreatRange = threatRange;
		mPsi = psi;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public DestroyedProbabilityDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public double getValue(Transition<IDurativeAction, DestroyedProbabilityDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		// Determining factors of effect on teamDestroyed variable
		TeamAltitude srcAlt = mDomain.getTeamAltitude(transition);
		TeamFormation srcForm = mDomain.getTeamFormation(transition);
		TeamECM srcECM = mDomain.getTeamECM(transition);
		ThreatDistribution threatDist = mDomain.getThreatDistribution(transition);

		double altTerm = Math.max(0, mThreatRange - srcAlt.getAltitudeLevel()) / mThreatRange;
		int phi = srcForm.getFormation().equals("loose") ? 0 : 1; // loose: phi = 0, tight: phi = 1
		double formTerm = (1 - phi) + phi / mPsi;
		int ecm = srcECM.isECMOn() ? 1 : 0;
		double ecmTerm = (1 - ecm) + ecm / 4.0;

		// Probability of being destroyed, given threat exists in the segment
		double destroyedProbGivenThreat = altTerm * formTerm * ecmTerm;

		// Probability of being destroyed
		double destroyedProb = threatDist.getExpectedThreatProbability() * destroyedProbGivenThreat;

		// DestroyedProbability value is only applicable when team is still alive
		TeamDestroyed srcDestroyed = mDomain.getTeamDestroyed(transition);
		return srcDestroyed.isDestroyed() ? 0 : destroyedProb;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof DestroyedProbabilityQFunction)) {
			return false;
		}
		DestroyedProbabilityQFunction qFunction = (DestroyedProbabilityQFunction) obj;
		return qFunction.mDomain.equals(mDomain);
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
