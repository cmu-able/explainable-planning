package examples.dart.models;

import language.domain.models.IStateVarAttribute;

/**
 * {@link TargetDistribution} is an attribute associated with a {@link RouteSegment} that represents a Beta-distribution
 * of target existing in the segment.
 * 
 * @author rsukkerd
 *
 */
public class TargetDistribution implements IStateVarAttribute {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private double mExpTargetProb;

	public TargetDistribution(double expTargetProb) {
		mExpTargetProb = expTargetProb;
	}

	public double getExpectedTargetProbability() {
		return mExpTargetProb;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof TargetDistribution)) {
			return false;
		}
		TargetDistribution targetDist = (TargetDistribution) obj;
		return Double.compare(targetDist.mExpTargetProb, mExpTargetProb) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Double.valueOf(mExpTargetProb).hashCode();
			hashCode = result;
		}
		return hashCode;
	}
}
