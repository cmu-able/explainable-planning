package language.exceptions;

import language.mdp.IStateVarTuple;
import language.qfactors.IAction;

public class ActionNotApplicableException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -8743806106331662830L;

	public ActionNotApplicableException(IAction action, IStateVarTuple predicate) {
		super("Action '" + action.getName() + "' is not applicable in " + predicate + " states.");
	}
}
