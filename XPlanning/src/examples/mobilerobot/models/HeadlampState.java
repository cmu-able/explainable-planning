package examples.mobilerobot.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarBoolean;
import language.exceptions.AttributeNameNotFoundException;

public class HeadlampState implements IStateVarBoolean {
	
	private boolean mOn;
	
	public HeadlampState(boolean on) {
		mOn = on;
	}

	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		throw new AttributeNameNotFoundException(name);
	}

	@Override
	public boolean getValue() {
		return mOn;
	}

	
	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof HeadlampState)) {
			return false;
		}
		HeadlampState hlstate = (HeadlampState) obj;
		return hlstate.mOn == mOn;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Boolean.hashCode(mOn);
			hashCode = result;
		}
		return hashCode;
	}

}
