package exceptions;

public class AttributeNameNotFoundException extends Exception {

	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = 8551472549457262722L;

	public AttributeNameNotFoundException(String name) {
		super("Attribute '" + name + "' is not found.");
	}
}
