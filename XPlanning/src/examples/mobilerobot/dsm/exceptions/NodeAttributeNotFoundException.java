package examples.mobilerobot.dsm.exceptions;

public class NodeAttributeNotFoundException extends MapTopologyException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7072241414191034238L;

	public NodeAttributeNotFoundException(String attributeName) {
		super("Node attribute '" + attributeName + "' is not found.");
	}
}
