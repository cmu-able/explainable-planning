package exceptions;

import factors.ActionDefinition;
import factors.IAction;

public class ActionDefinitionNotFoundException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -1958925124038439355L;

	public ActionDefinitionNotFoundException(ActionDefinition<? extends IAction> actionDefinition) {
		super("Action type '" + actionDefinition.getName() + "' is not found.");
	}
}