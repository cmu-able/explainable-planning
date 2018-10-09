package language.exceptions;

import language.domain.models.IStateVarValue;
import language.domain.models.StateVarDefinition;

public class IncompatibleVarException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -943991992432501006L;

	public IncompatibleVarException(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		super("State variable '" + stateVarDef.getName() + "' is incompatible.");
	}
}
