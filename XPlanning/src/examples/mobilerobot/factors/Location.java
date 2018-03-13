package examples.mobilerobot.factors;

import exceptions.AttributeNameNotFoundException;
import factors.IStateVarAttribute;
import factors.IStateVarValue;
import factors.StateVarValue;

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
	private String mId;

	public Location(String id, Area area) {
		mId = id;
		mValue = new StateVarValue();
		mValue.putAttributeValue("area", area);
	}

	public String getId() {
		return mId;
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
		return location.mId.equals(mId) && location.mValue.equals(mValue);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mValue.hashCode();
			result = 31 * result + mId.hashCode();
			hashCode = result;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "L" + mId;
	}
}
