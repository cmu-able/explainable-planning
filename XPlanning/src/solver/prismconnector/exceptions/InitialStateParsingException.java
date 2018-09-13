package solver.prismconnector.exceptions;

import java.util.List;

public class InitialStateParsingException extends ExplicitModelParsingException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4969473892757037975L;

	public InitialStateParsingException(String labHeader) {
		super("Cannot parse the \"init\" label from the header: " + labHeader);
	}

	public InitialStateParsingException(String labHeader, List<String> labBody) {
		super("Cannot find the initial state from: " + labHeader + "\n" + labBody);
	}
}
