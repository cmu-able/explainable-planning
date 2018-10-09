package language.domain.models;

import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link IStateVarValue} is an interface to a type of values that a state variable can take. Such a value can be an
 * atomic value or can have an associated set of attribute values. Multiple state variables can have the same value
 * type.
 * 
 * Note: For now, assume that all attributes have unique names.
 * 
 * @author rsukkerd
 *
 */
public interface IStateVarValue {

	public IStateVarAttribute getAttributeValue(String name) throws AttributeNameNotFoundException;
}
