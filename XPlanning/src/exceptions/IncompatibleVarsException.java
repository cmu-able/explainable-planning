package exceptions;

import mdp.IStatePredicate;

public class IncompatibleVarsException extends XMDPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -845363917405462333L;

	public IncompatibleVarsException(IStatePredicate statePredicate) {
		super("State variables in the predicate '" + statePredicate + "' are incompatible.");
	}

}
