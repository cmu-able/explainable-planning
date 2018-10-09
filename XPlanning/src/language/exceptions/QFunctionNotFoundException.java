package language.exceptions;

import language.domain.metrics.IQFunction;

public class QFunctionNotFoundException extends XMDPException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 4557752879934242335L;

	public QFunctionNotFoundException(IQFunction<?, ?> qFunction) {
		super("QA function '" + qFunction.getName() + "' is not found.");
	}
}
