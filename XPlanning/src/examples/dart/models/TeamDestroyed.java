package examples.dart.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarBoolean;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link TeamDestroyed} indicates whether the team has been shot down by a threat.
 * 
 * @author rsukkerd
 *
 */
public class TeamDestroyed implements IStateVarBoolean {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private boolean mDestroyed;

	public TeamDestroyed(boolean destroyed) {
		mDestroyed = destroyed;
	}

	public boolean isDestroyed() {
		return mDestroyed;
	}

	@Override
	public boolean getValue() {
		return isDestroyed();
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
		if (!(obj instanceof TeamDestroyed)) {
			return false;
		}
		TeamDestroyed destroyed = (TeamDestroyed) obj;
		return destroyed.mDestroyed == mDestroyed;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = Boolean.hashCode(mDestroyed);
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return Boolean.toString(mDestroyed);
	}

}
