package examples.clinicscheduling.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarInt;
import language.exceptions.AttributeNameNotFoundException;

public class AdvanceBookingPolicy implements IStateVarInt {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mABP;

	public AdvanceBookingPolicy(int abp) {
		mABP = abp;
	}

	public int getAdvanceBookingPolicy() {
		return mABP;
	}

	@Override
	public int getValue() {
		return getAdvanceBookingPolicy();
	}

	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof AdvanceBookingPolicy)) {
			return false;
		}
		AdvanceBookingPolicy abp = (AdvanceBookingPolicy) obj;
		return abp.mABP == mABP;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Integer.hashCode(mABP);
			hashCode = result;
		}
		return hashCode;
	}

}
