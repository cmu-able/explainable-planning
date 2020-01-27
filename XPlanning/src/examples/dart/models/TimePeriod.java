package examples.dart.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarInt;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link TimePeriod} is a discrete time period t of the mission. This corresponds to the segment t in the mission
 * route.
 * 
 * @author rsukkerd
 *
 */
public class TimePeriod implements IStateVarInt {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mPeriod;

	public TimePeriod(int period) {
		mPeriod = period;
	}

	public int getTimePeriod() {
		return mPeriod;
	}

	@Override
	public int getValue() {
		return getTimePeriod();
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
		if (!(obj instanceof TimePeriod)) {
			return false;
		}
		TimePeriod period = (TimePeriod) obj;
		return Integer.compare(period.mPeriod, mPeriod) == 0;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Integer.hashCode(mPeriod);
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return Integer.toString(mPeriod);
	}

}
