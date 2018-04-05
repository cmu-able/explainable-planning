package exceptions;

import mdp.DiscriminantClass;

public class IncompatibleDiscriminantClassException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 4097647380003233442L;

	public IncompatibleDiscriminantClassException(DiscriminantClass discriminantClass) {
		super("Discriminant class '" + discriminantClass + "' is incompatible.");
	}
}
