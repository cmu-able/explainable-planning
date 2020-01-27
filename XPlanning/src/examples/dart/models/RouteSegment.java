package examples.dart.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarInt;
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

	public RouteSegment(int segment) {
		mSegment = segment;
	}

	public int getSegment() {
		return mSegment;
	}

	@Override
	public int getValue() {
		return getSegment();
	}

	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		throw new AttributeNameNotFoundException(name);
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
		return Integer.compare(segment.mSegment, mSegment) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Integer.hashCode(mSegment);
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return Integer.toString(mSegment);
	}

}
