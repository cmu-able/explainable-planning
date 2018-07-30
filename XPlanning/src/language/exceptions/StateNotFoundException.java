package language.exceptions;

import language.mdp.StateVarTuple;

public class StateNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 2445394501462601722L;

	public StateNotFoundException(StateVarTuple state) {
		super("State '" + state + "' is not found.");
	}
}
