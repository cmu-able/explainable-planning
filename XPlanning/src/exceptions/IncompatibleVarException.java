package exceptions;

import factors.IStateVarValue;
import factors.StateVarDefinition;

public class IncompatibleVarException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -943991992432501006L;

	public IncompatibleVarException(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		super("State variable '" + stateVarDef.getName() + "' is incompatible.");
	}
}
