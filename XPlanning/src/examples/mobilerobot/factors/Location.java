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
}
