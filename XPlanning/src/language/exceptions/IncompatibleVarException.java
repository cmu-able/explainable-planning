package language.exceptions;

import language.qfactors.IStateVarValue;
import language.qfactors.StateVarDefinition;

public class IncompatibleVarException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -943991992432501006L;

	public IncompatibleVarException(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		super("State variable '" + stateVarDef.getName() + "' is incompatible.");
	}
}
