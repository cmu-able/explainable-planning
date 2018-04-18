package exceptions;

import factors.IAction;

public class IncompatibleActionException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -1937515700460914389L;

	public IncompatibleActionException(IAction action) {
		super("Action '" + action.getName() + "' is incompatible.");
	}
}
