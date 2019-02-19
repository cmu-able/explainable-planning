package solver.prismconnector.exceptions;

import java.util.Arrays;

public class ResultParsingException extends PrismConnectorException {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 7242126185460185387L;

	public ResultParsingException(String resultStr, String... regexs) {
		super("Cannot parse the result \"" + resultStr + "\" using the regular expression(s): "
				+ Arrays.toString(regexs));
	}
}
