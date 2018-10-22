package examples.clinicscheduling.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarInt;
import language.exceptions.AttributeNameNotFoundException;

public class ABP implements IStateVarInt {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private int mABP;

	public ABP(int abp) {
		mABP = abp;
	}

	public int getABP() {
		return mABP;
	}

	@Override
	public int getValue() {
		return getABP();
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
		if (!(obj instanceof ABP)) {
			return false;
		}
		ABP abp = (ABP) obj;
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

	@Override
	public String toString() {
		return Integer.toString(mABP);
	}

}
