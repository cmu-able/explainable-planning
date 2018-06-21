package exceptions;

import policy.PolicyMeta.SOLUTION_CODE;

public class NoSolutionException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 7253699755228067357L;

	public NoSolutionException(SOLUTION_CODE noSolutionCode) {
		super("No solution found: " + noSolutionCode);
	}
}
