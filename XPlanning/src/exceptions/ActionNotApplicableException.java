package exceptions;

import factors.IAction;
import mdp.IPredicate;

public class ActionNotApplicableException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -8743806106331662830L;

	public ActionNotApplicableException(IAction action, IPredicate predicate) {
		super("Action '" + action.getName() + "' is not applicable in " + predicate + " states.");
	}
}
