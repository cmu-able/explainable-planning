package factors;

import java.util.List;

import exceptions.AttributeNameNotFoundException;

/**
 * {@link IAction} is an interface to the types of actions. An action has a name and can have parameters, attribute
 * values, and derived attribute values.
 * 
 * @author rsukkerd
 *
 */
public interface IAction {

	public String getActionName();

	public List<IStateVar> getParameters();

	public IActionAttribute getAttributeValue(String name) throws AttributeNameNotFoundException;

	public IActionAttribute getDerivedAttributeValue(String name, IStateVar... srcStateVars)
			throws AttributeNameNotFoundException;
}
