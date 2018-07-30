package language.exceptions;

import language.mdp.Discriminant;

public class DiscriminantNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -2292379123941276428L;

	public DiscriminantNotFoundException(Discriminant discriminant) {
		super("Discriminant '" + discriminant + "' is not found.");
	}
}
