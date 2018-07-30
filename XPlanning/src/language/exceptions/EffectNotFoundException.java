package language.exceptions;

import language.mdp.Effect;

public class EffectNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 1700001233852459492L;

	public EffectNotFoundException(Effect effect) {
		super("Effect '" + effect + "' is not found.");
	}
}
