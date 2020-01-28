package examples.dart.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarInt;
import language.domain.models.StateVarValue;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link RouteSegment} is a segment in the mission route.
 * 
 * @author rsukkerd
 *
 */
public class RouteSegment implements IStateVarInt {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mSegment;
	private StateVarValue mValue;

	public RouteSegment(int segment, ThreatDistribution threatDist, TargetDistribution targetDist) {
		mSegment = segment;
		mValue = new StateVarValue("segment " + segment);
		mValue.putAttributeValue("threatDistribution", threatDist);
		mValue.putAttributeValue("targetDistribution", targetDist);
	}

	public int getSegment() {
		return mSegment;
	}

	public ThreatDistribution getThreatDistribution() throws AttributeNameNotFoundException {
		return (ThreatDistribution) getAttributeValue("threatDistribution");
	}

	public TargetDistribution getTargetDistribution() throws AttributeNameNotFoundException {
		return (TargetDistribution) getAttributeValue("targetDistribution");
	}

	@Override
	public int getValue() {
		return getSegment();
	}

	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		return mValue.getAttributeValue(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof RouteSegment)) {
			return false;
		}
		RouteSegment segment = (RouteSegment) obj;
		return Integer.compare(segment.mSegment, mSegment) == 0 && segment.mValue.equals(mValue);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + Integer.hashCode(mSegment);
			result = 31 * result + mValue.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return Integer.toString(mSegment);
	}

}
