package language.domain.models;

import java.util.List;
import java.util.Set;

import language.exceptions.AttributeNameNotFoundException;

/**
 * {@link IAction} is an interface to the types of actions. An action has a name and can have parameters, attribute
 * values, and derived attribute values.
 * 
 * @author rsukkerd
 *
 */
public interface IAction {

	public String getName();

	public String getNamePrefix();

	public List<IStateVarValue> getParameters();

	public IActionAttribute getAttributeValue(String name) throws AttributeNameNotFoundException;

	public IActionAttribute getDerivedAttributeValue(String name, Set<StateVar<? extends IStateVarValue>> srcStateVars)
			throws AttributeNameNotFoundException;
}
