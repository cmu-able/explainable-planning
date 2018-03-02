package factors;

import exceptions.AttributeNameNotFoundException;

/**
 * {@link IStateVarValue} is an interface to the types of values that the state variables can take. 
 * Such a value can be an atomic value or can have an associated set of attribute values.
 * 
 * @author rsukkerd
 *
 */
public interface IStateVarValue {

	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException;
}
