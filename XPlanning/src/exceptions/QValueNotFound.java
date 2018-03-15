package exceptions;

import mdp.Transition;

public class QValueNotFound extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 143402098401727166L;

	public QValueNotFound(Transition trans) {
		super("Q value for transition '" + trans.toString() + "' not found.");
	}
}
