package exceptions;

import mdp.State;

public class StateNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 2445394501462601722L;

	public StateNotFoundException(State state) {
		super("State '" + state + "' is not found.");
	}
}
