package language.exceptions;

import language.mdp.EffectClass;

public class EffectClassNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -7754271086719427413L;

	public EffectClassNotFoundException(EffectClass effectClass) {
		super("Effect class '" + effectClass + "' is not found.");
	}
}
