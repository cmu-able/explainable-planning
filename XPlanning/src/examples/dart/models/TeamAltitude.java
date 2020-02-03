package examples.dart.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarInt;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link TeamAltitude} is the type of altitude level of the team. The unit is a discrete level. It can be used in 2
 * cases: (1) as a state variable, denoting the altitude at which the team is flying, or (2) as an action parameter,
 * denoting the change in altitude level.
 * 
 * @author rsukkerd
 *
 */
public class TeamAltitude implements IStateVarInt, Comparable<TeamAltitude> {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mAltitudeLevel;

	public TeamAltitude(int altitudeLevel) {
		mAltitudeLevel = altitudeLevel;
	}

	public int getAltitudeLevel() {
		return mAltitudeLevel;
	}

	@Override
	public int getValue() {
		return getAltitudeLevel();
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
		if (!(obj instanceof TeamAltitude)) {
			return false;
		}
		TeamAltitude altitude = (TeamAltitude) obj;
		return Integer.compare(altitude.mAltitudeLevel, mAltitudeLevel) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Integer.hashCode(mAltitudeLevel);
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return Integer.toString(mAltitudeLevel);
	}

	@Override
	public int compareTo(TeamAltitude other) {
		return mAltitudeLevel - other.mAltitudeLevel;
	}

}
