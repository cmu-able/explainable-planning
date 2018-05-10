package exceptions;

import metrics.IQFunction;

public class QFunctionNotFoundException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 4557752879934242335L;

	public QFunctionNotFoundException(IQFunction qFunction) {
		super("QA function '" + qFunction.getName() + "' is not found.");
	}
}
