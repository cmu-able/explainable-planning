package language.exceptions;

import language.mdp.StateVarClass;

public class StateVarClassNotFoundException extends XMDPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7439849664136145853L;

	public StateVarClassNotFoundException(StateVarClass stateVarClass) {
		super("State variable class '" + stateVarClass + "' is not found");
	}

}
