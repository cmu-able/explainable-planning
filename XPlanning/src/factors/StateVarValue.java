package factors;

import java.util.HashMap;
import java.util.Map;

import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link StateVarValue} defines a generic type of values that a state variable can take. Its attribute values have a
 * generic {@link IStateVarAttribute} type.
 * 
 * @author rsukkerd
 *
 */
public class StateVarValue implements IStateVarValue {

	/*
	 * Cached hashCode -- Effective Java
	 */
	private volatile int hashCode;

	private Map<String, IStateVarAttribute> mAttributes = new HashMap<>();

	public StateVarValue() {
		// mAttributes initially empty
	}

	public void putAttributeValue(String name, IStateVarAttribute value) {
		mAttributes.put(name, value);
	}

	@Override
	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException {
		if (!mAttributes.containsKey(name)) {
			throw new AttributeNameNotFoundException(name);
		}
		return mAttributes.get(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof StateVarValue)) {
			return false;
		}
		StateVarValue value = (StateVarValue) obj;
		return value.mAttributes.equals(mAttributes);
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = 17;
			result = 31 * result + mAttributes.hashCode();
			hashCode = result;
		}
		return result;
	}

}
