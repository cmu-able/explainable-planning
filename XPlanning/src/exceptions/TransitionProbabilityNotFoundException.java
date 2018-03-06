package exceptions;

import factors.Transition;

public class TransitionProbabilityNotFoundException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -2129559636884515026L;

	public TransitionProbabilityNotFoundException(Transition trans) {
		super("Probability of transition '" + trans.toString() + "' is not found.");
	}
}
