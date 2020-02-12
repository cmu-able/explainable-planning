package examples.dart.metrics;

import examples.dart.models.IDurativeAction;
import examples.dart.models.TargetDistribution;
import examples.dart.models.TeamAltitude;
import examples.dart.models.TeamDestroyed;
import examples.dart.models.TeamECM;
import examples.dart.models.TeamFormation;
import language.domain.metrics.IEvent;
import language.domain.metrics.Transition;
import language.exceptions.AttributeNameNotFoundException;
import language.exceptions.VarNotFoundException;

/**
 * {@link MissTargetEvent} represent the team missing a target during IncAlt/DecAlt/Fly action, when the target actually
 * exists in the team's current segment.
 * 
 * @author rsukkerd
 *
 */
public class MissTargetEvent implements IEvent<IDurativeAction, DetectTargetDomain> {

	public static final String NAME = "missTarget";

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private DetectTargetDomain mDomain;
	private double mSensorRange; // at an altitude of r_S or higher, it is not possible to detect targets
	private double mSigma; // factor by which the detection probability is reduced due to flying in tight formation

	public MissTargetEvent(DetectTargetDomain domain, double sensorRange, double sigma) {
		mDomain = domain;
		mSensorRange = sensorRange;
		mSigma = sigma;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public DetectTargetDomain getTransitionStructure() {
		return mDomain;
	}

	@Override
	public double getEventProbability(Transition<IDurativeAction, DetectTargetDomain> transition)
			throws VarNotFoundException, AttributeNameNotFoundException {
		// Determining factors of missing target
		TeamDestroyed srcDestroyed = mDomain.getTeamDestroyed(transition);
		TeamAltitude srcAlt = mDomain.getTeamAltitude(transition);
		TeamFormation srcForm = mDomain.getTeamFormation(transition);
		TeamECM srcECM = mDomain.getTeamECM(transition);
		TargetDistribution targetDist = mDomain.getTargetDistribution(transition);
		IDurativeAction durative = transition.getAction();

		// If team is already destroyed, it cannot detect any target
		double destroyedTerm = srcDestroyed.isDestroyed() ? 0 : 1;

		// Use the average altitude of the team during the segment to compute probability of detecting target
		double avgAltitude = srcAlt.getAltitudeLevel();

		// If the action changes teamAltitude (i.e., incAlt and decAlt actions), compute the average altitude
		if (durative.getNamePrefix().equals("incAlt") || durative.getNamePrefix().equals("decAlt")) {
			// Get altitude change parameter
			TeamAltitude altChange = (TeamAltitude) durative.getParameters().get(0);

			int sign = durative.getNamePrefix().equals("incAlt") ? 1 : -1;
			avgAltitude += sign * altChange.getAltitudeLevel() / 2.0;
		}

		double altTerm = Math.max(0, mSensorRange - avgAltitude) / mSensorRange;
		int phi = srcForm.getFormation().equals("loose") ? 0 : 1; // loose: phi = 0, tight: phi = 1
		double formTerm = (1 - phi) + phi / mSigma;
		int ecm = srcECM.isECMOn() ? 1 : 0;
		double ecmTerm = (1 - ecm) + ecm / 4.0;

		// Probability of detecting target, given target exists in the segment
		double detectTargetProbGivenTarget = destroyedTerm * altTerm * formTerm * ecmTerm;

		// Probability of missing target, given target exists in the segment
		double missTargetProbGivenTarget = 1 - detectTargetProbGivenTarget;

		// Probability of missing target
		return targetDist.getExpectedTargetProbability() * missTargetProbGivenTarget;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof MissTargetEvent)) {
			return false;
		}
		MissTargetEvent event = (MissTargetEvent) obj;
		return event.mDomain.equals(mDomain) && Double.compare(event.mSensorRange, mSensorRange) == 0
				&& Double.compare(event.mSigma, mSigma) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mDomain.hashCode();
			result = 31 * result + Double.hashCode(mSensorRange);
			result = 31 * result + Double.hashCode(mSigma);
			hashCode = result;
		}
		return result;
	}

}
