package exceptions;

import mdp.StatePredicate;

public class StateNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 2445394501462601722L;

	public StateNotFoundException(StatePredicate state) {
		super("State '" + state + "' is not found.");
	}
}
