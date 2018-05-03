package exceptions;

import mdp.IPredicate;

public class PredicateNotFoundException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 2445394501462601722L;

	public PredicateNotFoundException(IPredicate predicate) {
		super("Predicate '" + predicate + "' is not found.");
	}
}
