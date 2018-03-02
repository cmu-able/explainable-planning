package factors;

import java.util.List;

import exceptions.AttributeNameNotFoundException;

/**
 * {@link IAction} is an interface to the types of actions. 
 * An action can have parameters, attribute values, and derived attribute values.
 * 
 * @author rsukkerd
 *
 */
public interface IAction {

	public List<IStateVarValue> getParameters();
	public IActionAttribute getAttributeValue(String name) throws AttributeNameNotFoundException;
	public IActionAttribute getDerivedAttributeValue(String name, IStateVarValue... srcStateVars) throws AttributeNameNotFoundException;
}
