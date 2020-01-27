package examples.dart.models;

import language.domain.models.IStateVarAttribute;

/**
 * {@link ThreatDistribution} is an attribute associated with a {@link RouteSegment} that represents a Beta-distribution
 * of threat existing in the segment.
 * 
 * @author rsukkerd
 *
 */
public class ThreatDistribution implements IStateVarAttribute {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double mExpThreatProb;

	public ThreatDistribution(double expThreatProb) {
		mExpThreatProb = expThreatProb;
	}

	public double getExpectedThreatProbability() {
		return mExpThreatProb;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ThreatDistribution)) {
			return false;
		}
		ThreatDistribution threatDist = (ThreatDistribution) obj;
		return Double.compare(threatDist.mExpThreatProb, mExpThreatProb) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Double.valueOf(mExpThreatProb).hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
