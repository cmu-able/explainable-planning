package language.exceptions;

import language.qfactors.IAction;

public class ActionNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 1234146259289944369L;

	public ActionNotFoundException(IAction action) {
		super("Action '" + action.getName() + "' is not found.");
	}
}
