package examples.mobilerobot.dsm.exceptions;

public class ConnectionAttributeNotFoundException extends MapTopologyException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -891938958921390972L;

	public ConnectionAttributeNotFoundException(String attributeName) {
		super("Connection attribute '" + attributeName + "' is not found.");
	}
}
