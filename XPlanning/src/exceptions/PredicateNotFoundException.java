package exceptions;

import policy.Predicate;

public class PredicateNotFoundException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 2445394501462601722L;

	public PredicateNotFoundException(Predicate predicate) {
		super("Predicate '" + predicate + "' is not found.");
	}
}
