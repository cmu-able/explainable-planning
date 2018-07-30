package language.exceptions;

import language.mdp.EffectClass;

public class IncompatibleEffectClassException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 4022655437563995066L;

	public IncompatibleEffectClassException(EffectClass effectClass) {
		super("Effect class '" + effectClass + "' is incompatible.");
	}
}
