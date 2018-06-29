package exceptions;

import factors.IStateVarValue;
import factors.StateVarDefinition;

public class VarNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -4877683215910076757L;

	public VarNotFoundException(StateVarDefinition<? extends IStateVarValue> stateVarDef) {
		super("State variable '" + stateVarDef.getName() + "' is not found.");
	}

	public VarNotFoundException(String stateVarName) {
		super("State variable '" + stateVarName + "' is not found.");
	}
}
