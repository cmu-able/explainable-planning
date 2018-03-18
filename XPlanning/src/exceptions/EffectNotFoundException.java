package exceptions;

import mdp.Effect;

public class EffectNotFoundException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 1700001233852459492L;

	public EffectNotFoundException(Effect effect) {
		super("Effect '" + effect + "' is not found.");
	}
}
