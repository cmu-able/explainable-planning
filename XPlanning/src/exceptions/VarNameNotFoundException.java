package exceptions;

public class VarNameNotFoundException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -4877683215910076757L;

	public VarNameNotFoundException(String name) {
		super("State variable '" + name + "' is not found.");
	}
}
