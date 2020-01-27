package examples.mobilerobot.models;

import language.domain.models.IStateVarAttribute;
import language.domain.models.IStateVarValue;
import language.domain.models.StateVarValue;
import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link Location} is a type of location values, which has a unique ID and an associated {@link Area}.
 * 
 * @author rsukkerd
 *
 */
public class Location implements IStateVarValue {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private StateVarValue mValue;

	public Location(String id, Area area) {
		mValue = new StateVarValue(id);
		mValue.putAttributeValue("area", area);
	}

	public String getId() {
		return mValue.getIdentifier();
	}

	public Area getArea() throws AttributeNameNotFoundException {
		return (Area) getAttributeValue("area");
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
		if (!(obj instanceof Location)) {
			return false;
		}
		Location location = (Location) obj;
		return location.mValue.equals(mValue);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mValue.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return mValue.getIdentifier();
	}
}
