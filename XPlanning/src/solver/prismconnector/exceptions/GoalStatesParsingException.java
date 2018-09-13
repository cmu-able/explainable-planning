package solver.prismconnector.exceptions;

import java.util.List;

public class GoalStatesParsingException extends ExplicitModelParsingException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1146072887350147984L;

	public GoalStatesParsingException(String labHeader) {
		super("Cannot parse the \"end\" label from the header: " + labHeader);
	}

	public GoalStatesParsingException(String labHeader, List<String> labBody) {
		super("Cannot find the end state from: " + labHeader + "\n" + labBody);
	}
}
