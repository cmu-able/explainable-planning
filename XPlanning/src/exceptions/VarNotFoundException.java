package exceptions;

import factors.IStateVarValue;
import factors.StateVarDefinition;

public class VarNotFoundException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -4877683215910076757L;

	public VarNotFoundException(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		super("State variable '" + stateVarDef.getName() + "' is not found.");
	}
}