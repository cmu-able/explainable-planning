package language.exceptions;

import language.mdp.IStateVarTuple;

public class IncompatibleVarsException extends XMDPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -845363917405462333L;

	public IncompatibleVarsException(IStateVarTuple statePredicate) {
		super("State variables in the predicate '" + statePredicate + "' are incompatible.");
	}

}
