package language.exceptions;

import language.qfactors.ActionDefinition;
import language.qfactors.IAction;

public class ActionDefinitionNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -1958925124038439355L;

	public ActionDefinitionNotFoundException(ActionDefinition<? extends IAction> actionDefinition) {
		super("Action type '" + actionDefinition.getName() + "' is not found.");
	}
}