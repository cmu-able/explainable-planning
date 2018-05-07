package exceptions;

public class ResultParsingException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 7242126185460185387L;

	public ResultParsingException(String resultStr, String regex) {
		super("Cannot parse the result " + resultStr + "using the regular expression " + regex);
	}
}
