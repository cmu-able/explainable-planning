package exceptions;

import mdp.EffectClass;

public class IncompatibleEffectClassException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 4022655437563995066L;

	public IncompatibleEffectClassException(EffectClass effectClass) {
		super("Effect class '" + effectClass + "' is incompatible.");
	}
}
