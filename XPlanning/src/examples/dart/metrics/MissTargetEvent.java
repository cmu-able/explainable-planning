package examples.dart.metrics;

import examples.dart.models.IDurativeAction;
import examples.dart.models.TargetDistribution;
import examples.dart.models.TeamAltitude;
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

	public static final String NAME = "target";

	private static final double SENSOR_RANGE = 2;
	private static final double SIGMA = 4;

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private DetectTargetDomain mDomain;

	public MissTargetEvent(DetectTargetDomain domain) {
		mDomain = domain;
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
		TeamAltitude srcAlt = mDomain.getTeamAltitude(transition);
		TeamFormation srcForm = mDomain.getTeamFormation(transition);
		TeamECM srcECM = mDomain.getTeamECM(transition);
		TargetDistribution targetDist = mDomain.getTargetDistribution(transition);

		double altTerm = Math.max(0, SENSOR_RANGE - srcAlt.getAltitudeLevel()) / SENSOR_RANGE;
		int phi = srcForm.getFormation().equals("loose") ? 0 : 1; // loose: phi = 0, tight: phi = 1
		double formTerm = (1 - phi) + phi / SIGMA;
		int ecm = srcECM.isECMOn() ? 1 : 0;
		double ecmTerm = (1 - ecm) + ecm / 4.0;

		// Probability of detecting target, given target exists in the segment
		double detectTargetProbGivenTarget = altTerm * formTerm * ecmTerm;

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
		return event.mDomain.equals(mDomain);
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