package factors;

import java.util.HashMap;
import java.util.Map;

import exceptions.AttributeNameNotFoundException;

/**
 * {@link StateVarValue} defines a type of values that the state variables can take.
 * 
 * @author rsukkerd
 *
 */
public class StateVarValue implements IStateVarValue {

	private Map<String, IStateVarAttribute> mAttributes;

	public StateVarValue() {
	}

	public void putAttributeValue(String name, IStateVarAttribute value) {
		if (mAttributes == null) {
			mAttributes = new HashMap<>();
		}
		mAttributes.put(name, value);
	}

	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		if (mAttributes == null || !mAttributes.containsKey(name)) {
			throw new AttributeNameNotFoundException(name);
		}
		return mAttributes.get(name);
	}

}
