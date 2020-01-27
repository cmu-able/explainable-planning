package examples.dart.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarBoolean;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link TeamECM} is the type of ECM usage of the team: on or off.
 * 
 * @author rsukkerd
 *
 */
public class TeamECM implements IStateVarBoolean {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private boolean mECM;

	public TeamECM(boolean ecm) {
		mECM = ecm;
	}

	public boolean isECMOn() {
		return mECM;
	}

	@Override
	public boolean getValue() {
		return isECMOn();
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
		if (!(obj instanceof TeamECM)) {
			return false;
		}
		TeamECM ecm = (TeamECM) obj;
		return ecm.mECM == mECM;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Boolean.hashCode(mECM);
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return Boolean.toString(mECM);
	}

}
